package jaci.gradle.deploy.discovery

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.RequestResultPair
import jaci.gradle.deploy.sessions.context.DeployContext
import org.apache.log4j.Logger

import javax.inject.Inject
import java.util.concurrent.TimeUnit

@CompileStatic
class TargetDiscoveryWorker implements Runnable {

    // Static Storage for Workers
    // In Gradle, we can't run a task in parallel using workers and pass non-serializable data
    // to the worker. To get around this, we store them statically and clear them at the conclusion
    // of the build. It's not at all advised, but it's the best we've got.

    // TODO: Switch this to a map

    private static Map<Integer, RequestResultPair<DiscoveryAction, DiscoveryResult>> storage = new HashMap<>()

    static void clearStorage() {
        storage.clear()
    }

    static int submitStorage(DiscoveryAction request) {
        storage.put(request.hashCode(), new RequestResultPair<>(request, null))
        return request.hashCode()
    }

    static DiscoveryResult obtainStorage(DiscoveryAction request) {
        return storage.remove(request.hashCode()).result
    }

    // Begin Worker

    RequestResultPair<DiscoveryAction, DiscoveryResult> pair
    Logger log

    @Inject
    DiscoverTargetWorker(Integer hashcode) {
        this.pair = storage.get(hashcode)
        log = Logger.getLogger("TargetDiscoveryWorker[${pair.request.toString()}]")
    }

    @Override
    void run() {
        launch()
    }

    void succeed(DeployContext ctx) {
        pair.result = new DiscoveryResult(pair.request.deployLocation, pair.request.state, null, ctx)
    }

    void fail(DiscoveryFailedException ex) {
        pair.result = new DiscoveryResult(pair.request.deployLocation, pair.request.state, ex, null)
    }

    void launch() {
        def thread = new Thread({ discover() })
        thread.start()

        def action = pair.request
        def target = action.deployLocation.target

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
                        def tEx = new DiscoveryFailedException(action, new InterruptedException("Connection Timed Out"))
                        fail(tEx)
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

    void discover() {
        log.info("Discovery thread started")

        def action = pair.request
        def target = action.deployLocation.target
        try {
            def ctx = action.discover()

            def toConnect = target.enabled(ctx)
            if (!toConnect) {
                throw new TargetVerificationException("Target failed enabled (onlyIf) check!")
            }

            // Target is valid, put it in the storage
            log.info("Target valid, putting in address storage...")
            succeed(ctx)
            log.info("Signalling Countdown")
            target.latch.countDown()
        } catch (InterruptedException ignored) {
            log.info("Thread interrupted!")
            Thread.currentThread().interrupt()
        } catch (Throwable e) {
            // Put this error into the failureStorage so it can be echo'd by the main thread
            // (workers can't print to stdout)
            def tEx = new DiscoveryFailedException(action, e)
            fail(tEx)
            log.info("Throwable caught in discovery thread")

            def s = new StringWriter()
            def pw = new PrintWriter(s)
            tEx.printStackTrace(pw)
            log.info(s.toString())
        }
    }

    @CompileStatic
    @InheritConstructors
    public static class TargetVerificationException extends RuntimeException { }
}
