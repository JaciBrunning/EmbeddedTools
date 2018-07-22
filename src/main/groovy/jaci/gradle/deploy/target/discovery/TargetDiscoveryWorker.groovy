package jaci.gradle.deploy.target.discovery

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.discovery.action.DiscoveryAction
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory

import javax.inject.Inject
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

@CompileStatic
class TargetDiscoveryWorker implements Runnable {

    // Static Storage for Workers
    // In Gradle, we can't run a task in parallel using workers and pass non-serializable data
    // to the worker. To get around this, we store them statically and clear them at the conclusion
    // of the build. It's not at all advised, but it's the best we've got.

    @CompileStatic
    @Canonical
    private static class DiscoveryStorage {
        RemoteTarget target
        Consumer<DeployContext> contextSet
    }

    private static Map<Integer, DiscoveryStorage> storage = [:]

    static void clearStorage() {
        storage.clear()
    }

    static int submitStorage(RemoteTarget target, Consumer<DeployContext> cb) {
        int hashcode = target.hashCode()
        storage.put(hashcode, new DiscoveryStorage(target, cb))
        return hashcode
    }

    static int storageCount() {
        return storage.size()
    }

    // Begin Worker

    RemoteTarget target
    Consumer<DeployContext> callback
    private ETLogger log

    TargetDiscoveryWorker(RemoteTarget target, Consumer<DeployContext> cb) {
        this.target = target
        this.callback = cb
        this.log = ETLoggerFactory.INSTANCE.create("${this.class.simpleName}[${target.name}]")
    }

    TargetDiscoveryWorker(DiscoveryStorage store) {
        this(store.target, store.contextSet)
    }

    @Inject
    TargetDiscoveryWorker(int hashcode) {
        this(storage.get(hashcode))
        storage.remove(hashcode)
    }

    @Override
    void run() {
        def actions = target.locations.collect { it.createAction() } as Set<DiscoveryAction>
        def exec = Executors.newFixedThreadPool(actions.size())

        try {
            // At least one thread was successful.
            DeployContext ctx = exec.invokeAny(actions, target.timeout, TimeUnit.SECONDS)
            succeeded(ctx)
        } catch (TimeoutException | ExecutionException ignored) {
            // No threads were successful
            def ex = actions.collect { DiscoveryAction action ->
                // If the action didn't throw an exception, it has timed out.
                action.getException() ?: new DiscoveryFailedException(action, new TimeoutException("Discovery timed out."))
            }
            failed(ex)
        } finally {
            if (log.backingLogger().infoEnabled) {
                def ex = actions.collect { DiscoveryAction action ->
                    action.getException()
                }.findAll { it != null }
                logAllExceptions(ex)
            }
        }
    }

    private void succeeded(DeployContext ctx) {
        log.log("Using ${ctx.friendlyString()} for target ${target.name}")
        callback.accept(ctx)
    }

    private void failed(List<DiscoveryFailedException> ex) {
        callback.accept(null)
        log.withLock {
            printFailures(ex)
            def failMsg = "Target ${target.name} could not be found at any location! See above for more details."
            if (target.failOnMissing)
                throw new TargetNotFoundException(failMsg)
            else {
                log.log(failMsg)
                log.log("${target.name}.failOnMissing is set to false. Skipping this target and moving on...")
            }
        }
    }

    private void logAllExceptions(List<DiscoveryFailedException> exceptions) {
        exceptions.each { DiscoveryFailedException ex ->
            log.info("Exception caught in discovery ${ex.action.deployLocation.friendlyString()}: ")
            def s = new StringWriter()
            def pw = new PrintWriter(s)
            ex.printStackTrace(pw)
            log.info(s.toString())
        }
    }

    private void printFailures(List<DiscoveryFailedException> failures) {
        def enumMap = new HashMap<DiscoveryState, List<DiscoveryFailedException>>()
        // Sort failures into state buckets
        failures.each { DiscoveryFailedException e ->
            if (!enumMap.containsKey(e.action.state))
                enumMap.put(e.action.state, [] as List)
            enumMap.get(e.action.state).add(e)
        }

        log.debug("Failures: ${enumMap}")
        // Sort and iterate by state priority
        def sorted = enumMap.keySet().sort { a -> -a.priority }
        int printFullPriority = sorted.first().priority

        sorted.each { DiscoveryState state ->
            List<DiscoveryFailedException> fails = enumMap[state]
            if (state.priority == printFullPriority || log.backingLogger().isInfoEnabled()){
                fails.each { DiscoveryFailedException failed ->
                    log.logErrorHead("${failed.action.deployLocation.friendlyString()}: ${state.stateLocalized.capitalize()}.")
                    log.push().with {
                        logError("Reason: ${failed.cause.class.simpleName}")
                        logError(failed.cause.message)
                    }
                }
            } else {
                log.logErrorHead("${fails.size()} other action(s) ${state.stateLocalized}.")
            }
        }

        log.log("Run with --info for more details")

        log.log("") // blank line

    }

}
