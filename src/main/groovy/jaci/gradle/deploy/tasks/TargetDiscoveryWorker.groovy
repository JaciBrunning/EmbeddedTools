package jaci.gradle.deploy.tasks

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.ETLogger
import jaci.gradle.RefcountList
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.context.SshDeployContext
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.transport.SshSessionController
import org.apache.log4j.Logger
import org.gradle.api.Project

import javax.inject.Inject
import java.util.concurrent.TimeUnit

@CompileStatic
class TargetDiscoveryWorker implements Runnable {

    // Static Storage for Workers
    // In Gradle, we can't run a task in parallel using workers and pass non-serializable data
    // to the worker. To get around this, we store them statically and clear them at the conclusion
    // of the build. It's not at all advised, but it's the best we've got.

    @Canonical
    @CompileStatic
    private static class DiscoveryStorage {
        ETLogger logger
        Project project
        RemoteTarget target
    }

    @Canonical
    @CompileStatic
    public static class DiscoveryResult {
        RemoteTarget target
        DiscoveryState state
        String fullAddress

        // On failure
        TargetFailedException failure

        // On success
        String host
        int port
        DeployContext ctx
    }

    private static RefcountList<DiscoveryStorage> discoveryStorage = new RefcountList<>()
    private static RefcountList<DiscoveryResult> resultStorage = new RefcountList<>()

    public static void clearStorage() {
        discoveryStorage.clear()
        resultStorage.clear()
    }

    public static void lock() {
        discoveryStorage.use()
        resultStorage.use()
    }

    public static void unlock() {
        discoveryStorage.release()
        resultStorage.release()
    }

    public static int submitStorage(ETLogger logger, Project project, RemoteTarget target) {
        return discoveryStorage.put(new DiscoveryStorage(logger, project, target))
    }

    public static List<DiscoveryResult> getResults(RemoteTarget target) {
        return resultStorage.get().findAll { it.target.equals(target) }
    }

    public static List<DiscoveryResult> getSuccesses(RemoteTarget target) {
        return resultStorage.get().findAll {
            it.target.equals(target) && it.failure == null
        }.sort {
            target.addresses.indexOf(it.fullAddress)
        }
    }

    public static List<DiscoveryResult> getFailures(RemoteTarget target) {
        return resultStorage.get().findAll {
            it.target.equals(target) && it.failure != null
        }.sort {
            target.addresses.indexOf(it.fullAddress)
        }
    }

    // Begin Worker

    String fullAddress
    int index

    Logger log
    DiscoveryState state

    @Inject
    DiscoverTargetWorker(String fullAddress, Integer index) {
        this.fullAddress = fullAddress
        this.index = index
        log = Logger.getLogger("DiscoverTargetWorker[" + fullAddress + "]")
    }

    @Override
    void run() {
        def storage = discoveryStorage.get(index)
        launch(storage.target, storage.project, storage.logger)
    }

    void submitResultFail(RemoteTarget target, TargetFailedException ex) {
        resultStorage.put(new DiscoveryResult(target, state, fullAddress, ex, null, 0, null))
    }

    void submitResultSucceed(RemoteTarget target, String host, int port, DeployContext ctx) {
        resultStorage.put(new DiscoveryResult(target, state, fullAddress, null, host, port, ctx))
    }

    void launch(RemoteTarget target, Project project, ETLogger deployLogger) {
        def thread = new Thread({ discover(target, project, deployLogger) })
        thread.start()

        try {
            // If a valid address is found, all other discovery threads should be halted.
            if (target.discoverInstant) {
                // Add 500 to account for Thread spinup (address resolution etc)
                boolean timedOut = !target.latch.await(target.timeout * 1000 + 500, TimeUnit.MILLISECONDS)
                boolean threadAlive = thread.isAlive()

                // Either latch has triggered, or we've reached timeout, so kill the thread
                if (threadAlive) {
                    thread.interrupt()
                    log.info("Interrupting discovery thread (${timedOut ? "Timed Out" : "Other Address Found"})")

                    if (timedOut) {
                        def tEx = new TargetFailedException(target, fullAddress, state, new InterruptedException("Connection Timed Out"))
                        submitResultFail(target, tEx)
                    }
                } else {
                    log.info("Discovery thread finished early (likely errored)")
                }
            } else {
                thread.join()
            }
        } catch (Exception e) {
            log.info("Unknown exception in thread management")

            def s = new StringWriter()
            def pw = new PrintWriter(s)
            e.printStackTrace(pw)
            log.info(s.toString())
        }
    }

    void discover(RemoteTarget target, Project project, ETLogger deployLogger) throws TargetFailedException {
        state = DiscoveryState.NOT_RESOLVED

        try {
            log.info("Discovery thread started")

            // Split host into host:port, using 22 as the default port if none provided
            def splitHost = fullAddress.split(":")
            def hostname = splitHost[0]
            def port = splitHost.length > 1 ? Integer.parseInt(splitHost[1]) : 22
            log.info("Parsed Host: HOST = ${hostname}, PORT = ${port}")

            def resolvedHost = resolveHostname(hostname, target.ipv6)
            state = DiscoveryState.RESOLVED

            // Attempt to connect to host via SSH
            def session = new SshSessionController(resolvedHost, port, target.user, target.password, target.timeout)
            log.info("Found ${resolvedHost}! (${fullAddress})")
            state = DiscoveryState.CONNECTED

            // Ensure the target succeeds in its connection tests
            def ctx = new SshDeployContext(project, target, resolvedHost, deployLogger, session, target.directory)
            def toConnect = target.toConnect(ctx)
            if (!toConnect) {
                throw new TargetVerificationException("Target failed toConnect (onlyIf) check!")
            }
            state = DiscoveryState.VERIFIED

            // Target is valid, put it in the storage
            log.info("Target valid, putting in address storage...")
            submitResultSucceed(target, resolvedHost, port, ctx)
            log.info("Signalling Countdown")
            target.latch.countDown()
        } catch (InterruptedException ignored) {
            log.info("Thread interrupted!")
            Thread.currentThread().interrupt()
        } catch (Throwable e) {
            // Put this error into the failureStorage so it can be echo'd by the main thread
            // (workers can't print to stdout)
            def tEx = new TargetFailedException(target, fullAddress, state, e)
            submitResultFail(target, tEx)
            log.info("Throwable caught in discovery thread")

            def s = new StringWriter()
            def pw = new PrintWriter(s)
            tEx.printStackTrace(pw)
            log.info(s.toString())
        }
    }

    String resolveHostname(String hostname, boolean allowIpv6) {
        String resolvedHost = hostname
        boolean hasResolved = false
        for (InetAddress addr : InetAddress.getAllByName(hostname)) {
            if (!addr.isMulticastAddress()) {
                if (!allowIpv6 && addr instanceof Inet6Address) {
                    log.info("Resolved address ${addr.getHostAddress()} ignored! (IPv6)")
                } else {
                    log.info("Resolved ${addr.getHostAddress()}")
                    resolvedHost = addr.getHostAddress()
                    hasResolved = true
                    break;
                }
            }
        }
        if (!hasResolved) {
            log.info("No host resolution! Using original...")
        }
        return resolvedHost
    }

    @CompileStatic
    public static class TargetFailedException extends RuntimeException {
        RemoteTarget target
        String host
        DiscoveryState state

        public TargetFailedException(RemoteTarget target, String host, DiscoveryState state, Throwable cause) {
            super(cause)
            this.target = target
            this.host = host
            this.state = state
        }
    }

    @CompileStatic
    @InheritConstructors
    public static class TargetVerificationException extends RuntimeException { }
}
