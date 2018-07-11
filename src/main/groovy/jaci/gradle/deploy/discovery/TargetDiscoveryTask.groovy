package jaci.gradle.deploy.discovery

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.ETLogger
import jaci.gradle.deploy.discovery.action.DiscoveryAction
import jaci.gradle.deploy.sessions.context.DeployContext
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

@CompileStatic
class TargetDiscoveryTask extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    private ETLogger log
    private boolean successful = false
    private DeployContext activeContext

    @Input
    RemoteTarget target

    @Inject
    TargetDiscoveryTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    public boolean available() {
        return successful
    }

    public DeployContext activeContext() {
        if (successful)
            return activeContext
        else
            throw new GradleException("This target is not available.")
    }

    @TaskAction
    void discoverTarget() {
        // Ask for password if needed
        log = new ETLogger("TargetDiscoveryTask[${target.name}]", services)

        log.log("Discovering Target ${target.name}")
        List<DiscoveryAction> actions = target.locations.collect { it.createAction() } as List<DiscoveryAction>

        actions.each { DiscoveryAction action ->
            log.debug("Action ${action.toString()}")

            log.debug("Submitting storage...")
            int hashcode = TargetDiscoveryWorker.submitStorage(action)
            log.debug("Submitted (${hashcode})")

            log.debug("Submitting Worker...")
            workerExecutor.submit(TargetDiscoveryWorker, { WorkerConfiguration config ->
                config.isolationMode = IsolationMode.NONE
                config.params hashcode
            } as Action)
        }

        log.debug("Workers submitted, awaiting...")
        workerExecutor.await()
        log.debug("Workers done!")

        def results = actions
                        .collect    { TargetDiscoveryWorker.obtainStorage(it) }
                        .sort       { l0 -> target.locations.findIndexOf { l1 -> l1.equals(l0) } }
                        .findAll    { it != null }

        def failed      = results.findAll { it.failure != null }
        def succeeded   = results.findAll { it.failure == null }

        log.debug("Results collected: ${failed.size()} failed, ${succeeded.size()} succeeded")

        if (succeeded.size() > 0) {
            success(succeeded)
        } else {
            failure(failed)
        }
    }

    private void success(List<DiscoveryResult> successes) {
        activeContext = successes.first().context
        successful = true
        log.log("Using ${activeContext.controller.friendlyString()} for target ${target.name}")
    }

    private void failure(List<DiscoveryResult> failures) {
        successful = false
        printFailures(failures)

        def failMsg = "Target ${target.name} could not be found at any location! See above for more details."
        if (target.failOnMissing)
            throw new TargetNotFoundException(failMsg)
        else {
            log.log(failMsg)
            log.log("${target.name}.failOnMissing is set to false. Skipping this target and moving on...")
        }
    }

    private void printFailures(List<DiscoveryResult> failures) {
        def enumMap = new HashMap<DiscoveryState, List<DiscoveryFailedException>>()
        // Sort failures into state buckets
        failures.each { DiscoveryResult e ->
            if (!enumMap.containsKey(e.state))
                enumMap.put(e.state, [] as List)
            enumMap.get(e.state).add(e.failure)
        }

        log.debug("Failure enum map: " + enumMap)
        log.debug("Failure types: " + enumMap.keySet().sort { a -> -a.priority })

        // Sort and iterate by state priority
        boolean printFull = true
        enumMap.keySet().sort { a -> -a.priority }.each { DiscoveryState state ->
            List<DiscoveryFailedException> fails = enumMap[state]
            if (!printFull) {
                log.log("${fails.size()} other address(es) ${state.stateLocalized}.")
            } else {
                fails.each { DiscoveryFailedException failed ->
                    log.logErrorHead("${failed.action.deployLocation.friendlyString()}: ${state.stateLocalized.capitalize()}.")
                    log.push().with {
                        logError("Reason: ${failed.cause.class.simpleName}")
                        logError(failed.cause.message)
                    }
                }
            }

            printFull = project.hasProperty("deploy-more-addr") || log.backingLogger().isInfoEnabled()
        }
        log.log("")
        log.log("Run with -Pdeploy-more-addr or --info for more details") // Blank line
    }

    @CompileStatic
    @InheritConstructors
    public static class TargetNotFoundException extends RuntimeException { }
}
