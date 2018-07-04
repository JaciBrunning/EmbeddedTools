package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.ETLogger
import jaci.gradle.EmbeddedTools
import jaci.gradle.WorkerStorage
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.DryDeployContext
import jaci.gradle.deploy.SshDeployContext
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.transport.SshSessionController
import org.apache.log4j.Logger
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject
import java.util.concurrent.TimeUnit

@CompileStatic
class TargetDiscoveryTask extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    // Project and TargetStorage are sent TO the workers, containing information
    // about the project and the target
    @Internal
    private static WorkerStorage<ProjectStorage> projectStorage = WorkerStorage.obtain()
    @Internal
    private static WorkerStorage<RemoteTarget>   targetStorage  = WorkerStorage.obtain()

    // Address storage is returned FROM the workers, containing all the successful
    // targets reached.
    @Internal
    private static WorkerStorage<AddressStorage> addressStorage = WorkerStorage.obtain()
    // Failure storage is returned FROM the workers, containing all the exceptions
    // that have lead to failed discoveries of targets.
    @Internal
    private static WorkerStorage<TargetFailedException> failureStorage = WorkerStorage.obtain()

    @Internal
    private static int storageRefcount = 0;

    public static void clearStorage() {
        projectStorage.clear()
        targetStorage.clear()
        addressStorage.clear()
        failureStorage.clear()
    }

    @Internal
    private ETLogger log
    @Internal
    private DeployContext context
    @Internal
    private boolean isActive

    @Input
    RemoteTarget target

    @Inject
    TargetDiscoveryTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    public static enum DiscoveryState {
        NOT_RESOLVED("Not Resolved", 0),
        RESOLVED("Not Connected", 1),
        CONNECTED("Connected but Invalid", 2),
        VERIFIED("Valid", 3);

        String stateLocalized
        int priority
        DiscoveryState(String local, int pri) {
            this.stateLocalized = local
            this.priority = pri
        }
    };

    public DeployContext getContext() {
        return context;
    }

    @TaskAction
    void discoverTarget() {
        // Ask for password if needed
        log = new ETLogger("TargetDiscoveryTask[${target.name}]", services)

        if (EmbeddedTools.isDryRun(project)) {
            log.log("Dry Run! Using ${target.addresses.first()} for target ${target.name}")
            context = new DryDeployContext(project, target, target.addresses.first(), log, target.directory)
            isActive = true
        } else {
            def password = target.password ?: ""
            if (target.promptPassword) {
                def tpassword = EmbeddedTools.promptPassword(target.user)
                if (tpassword != null) password = tpassword
            }

            // We only have to prompt once
            target.password = password
            target.promptPassword = false

            log.log("Discovering Target ${target.name}")

            // Assertions
            assert target.user != null
            assert target.timeout > 0

            storageRefcount++
            log.debug("Storage Refcount Obtained: ${storageRefcount}")

            // Push project and target info into storage
            def projIndex = projectStorage.put(new ProjectStorage(project, log))
            def targIndex = targetStorage.put(target)

            // Submit some Workers on the Worker API to test addresses. This allows the task to run in parallel
            log.debug("Submitting workers...")
            target.addresses.each { String addr ->
                workerExecutor.submit(DiscoverTargetWorker, ({ WorkerConfiguration config ->
                    config.isolationMode = IsolationMode.NONE
                    config.params addr, targIndex, projIndex
                } as Action))
            }
            // Wait for all workers to complete
            log.debug("Awaiting workers...")
            workerExecutor.await()
            log.debug("Workers done!")

            boolean targetReachable = !addressStorage.findAll {
                it.target.equals(target)
            }.isEmpty()

            log.debug("Reachable = ${targetReachable}")
            if (!targetReachable) {
                isActive = false
                printFailures()
            } else {
                def active = activeAddress()

                log.log("Using address ${active.address}:${active.port} for target ${target.name}")
                context = active.ctx
                isActive = true
            }

            // Let the refcounts clear before we end this task
            storageRefcount--
            log.debug("Storage Refcount Released: ${storageRefcount}")
            if (storageRefcount <= 0) {
                log.info("Clearing discovery storage (refcount=0)")
                clearStorage()
            }

            if (!targetReachable) {
                String failureMessage = "Target ${target.name} could not be found! See above for more details."
                if (target.failOnMissing)
                    throw new TargetNotFoundException(failureMessage)
                else
                    log.log(failureMessage)
            }
        }
    }

    void printFailures() {
        def failures = getFailures()
        def enumMap = new HashMap<DiscoveryState, List<TargetFailedException>>()
        // Sort failures into state buckets
        failures.each { TargetFailedException e ->
            if (!enumMap.containsKey(e.state))
                enumMap.put(e.state, [] as List)
            enumMap.get(e.state).add(e)
        }

        // Sort and iterate by state priority
        boolean printFull = true
        enumMap.keySet().sort { a -> -a.priority }.each { DiscoveryState state ->
            List<TargetFailedException> fails = enumMap[state]
            if (!printFull) {
                log.log("${fails.size()} other address(es) ${state.stateLocalized}.")
            } else {
                fails.each { TargetFailedException failed ->
                    log.logErrorHead("Address ${failed.host}: ${state.stateLocalized}.")
                    log.push().with {
                        logError("Reason: ${failed.cause.class.simpleName}")
                        logError(failed.cause.message)
                    }
                }
                log.log("")
            }

            printFull = project.hasProperty("deploy-more-addr") || log.backingLogger().isInfoEnabled()
        }
        log.log("Run with -Pdeploy-more-addr or --info for more details") // Blank line
    }

    public boolean isTargetActive() {
        return isActive
    }

    @Internal
    private AddressStorage activeAddress() {
        return addressStorage.findAll {
            it.target.equals(target)
        }.sort { AddressStorage addrStor ->
            target.addresses.indexOf(addrStor.address)
        }.first()        // Order based on what order addresses registered
    }

    @Internal
    private List<TargetFailedException> getFailures() {
        return failureStorage.findAll {
            it.target.equals(target)
        }
    }

    static class DiscoverTargetWorker implements Runnable {
        String host
        int targetIndex, projectIndex

        Logger log
        DiscoveryState state

        @Inject
        DiscoverTargetWorker(String host, Integer targetIndex, Integer projectIndex) {
            this.targetIndex = targetIndex
            this.projectIndex = projectIndex
            this.host = host
            log = Logger.getLogger("DiscoverTargetWorker[" + host + "]")
        }

        @Override
        void run() {
            def target = targetStorage.get(targetIndex)
            def projectContainer = projectStorage.get(projectIndex)
            def thread = new Thread({ discover(target, projectContainer) });
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
                            def tEx = new TargetFailedException(target, host, state, new InterruptedException("Connection Timed Out"))
                            failureStorage.push(tEx)
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

        void discover(RemoteTarget target, ProjectStorage projStore) throws TargetFailedException {
            state = DiscoveryState.NOT_RESOLVED

            try {
                log.info("Discovery thread started")

                // Split host into host:port, using 22 as the default port if none provided
                def splitHost = host.split(":")
                def hostname = splitHost[0]
                def port = splitHost.length > 1 ? Integer.parseInt(splitHost[1]) : 22
                log.info("Parsed Host: HOST = ${hostname}, PORT = ${port}")

                def resolvedHost = resolveHostname(hostname, target.ipv6)
                state = DiscoveryState.RESOLVED

                // Attempt to connect to host via SSH
                def session = new SshSessionController(resolvedHost, port, target.user, target.password, target.timeout)
                log.info("Found ${resolvedHost}! (${host})")
                state = DiscoveryState.CONNECTED

                // Ensure the target succeeds in its connection tests
                def ctx = new SshDeployContext(projStore.project, target, resolvedHost, projStore.deployLogger, session, target.directory)
                def toConnect = target.toConnect(ctx)
                if (!toConnect) {
                    throw new TargetVerificationException("Target failed toConnect (onlyIf) check!")
                }
                state = DiscoveryState.VERIFIED

                // Target is valid, put it in the storage
                log.info("Target valid, putting in address storage...")
                addressStorage.push(new AddressStorage(host, port, target, ctx))
                log.info("Signalling Countdown")
                target.latch.countDown()
            } catch (InterruptedException ignored) {
                log.info("Thread interrupted!")
                Thread.currentThread().interrupt()
            } catch (Throwable e) {
                // Put this error into the failureStorage so it can be echo'd by the main thread
                // (workers can't print to stdout)
                def tEx = new TargetFailedException(target, host, state, e)
                failureStorage.push(tEx)
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
    public static class TargetNotFoundException extends RuntimeException { }

    @CompileStatic
    @InheritConstructors
    public static class TargetVerificationException extends RuntimeException { }

    @CompileStatic
    private static class AddressStorage {
        String address
        int port
        RemoteTarget target
        DeployContext ctx

        AddressStorage(String addr, int port, RemoteTarget target, DeployContext ctx) {
            this.address = addr;
            this.port = port
            this.target = target;
            this.ctx = ctx;
        }
    }

    @CompileStatic
    private static class ProjectStorage {
        Project project
        ETLogger deployLogger

        ProjectStorage(Project project, ETLogger log) {
            this.project = project
            this.deployLogger = log
        }
    }
}
