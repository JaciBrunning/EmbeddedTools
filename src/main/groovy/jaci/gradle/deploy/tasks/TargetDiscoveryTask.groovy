package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.ETLogger
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.context.DryDeployContext
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Action
import org.gradle.api.DefaultTask
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

            TargetDiscoveryWorker.lock()

            try {
                // Push project and target info into storage
                def index = TargetDiscoveryWorker.submitStorage(log, project, target)

                // Submit some Workers on the Worker API to test addresses. This allows the task to run in parallel
                log.debug("Submitting workers...")
                target.addresses.each { String addr ->
                    workerExecutor.submit(TargetDiscoveryWorker, ({ WorkerConfiguration config ->
                        config.isolationMode = IsolationMode.NONE
                        config.params addr, index
                    } as Action))
                }
                // Wait for all workers to complete
                log.debug("Awaiting workers...")
                workerExecutor.await()
                log.debug("Workers done!")

                boolean targetReachable = !getSuccesses().isEmpty()

                log.debug("Reachable = ${targetReachable}")
                if (!targetReachable) {
                    isActive = false
                    printFailures()
                } else {
                    def active = activeAddress()

                    log.log("Using address ${active.host}:${active.port} for target ${target.name}")
                    context = active.ctx
                    isActive = true
                }

                if (!targetReachable) {
                    String failureMessage = "Target ${target.name} could not be found! See above for more details."
                    if (target.failOnMissing)
                        throw new TargetNotFoundException(failureMessage)
                    else
                        log.log(failureMessage)
                }
            } finally {
                TargetDiscoveryWorker.unlock()
            }
        }
    }

    void printFailures() {
        def failures = getFailures()
        def enumMap = new HashMap<DiscoveryState, List<TargetDiscoveryWorker.TargetFailedException>>()
        // Sort failures into state buckets
        failures.each { TargetDiscoveryWorker.DiscoveryResult e ->
            if (!enumMap.containsKey(e.state))
                enumMap.put(e.state, [] as List)
            enumMap.get(e.state).add(e.failure)
        }

        // Sort and iterate by state priority
        boolean printFull = true
        enumMap.keySet().sort { a -> -a.priority }.each { DiscoveryState state ->
            List<TargetDiscoveryWorker.TargetFailedException> fails = enumMap[state]
            if (!printFull) {
                log.log("${fails.size()} other address(es) ${state.stateLocalized}.")
            } else {
                fails.each { TargetDiscoveryWorker.TargetFailedException failed ->
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
    private TargetDiscoveryWorker.DiscoveryResult activeAddress() {
        return getSuccesses().first()
    }

    @Internal
    private List<TargetDiscoveryWorker.DiscoveryResult> getSuccesses() {
        return TargetDiscoveryWorker.getSuccesses(target)
    }

    @Internal
    private List<TargetDiscoveryWorker.DiscoveryResult> getFailures() {
        return TargetDiscoveryWorker.getFailures(target)
    }

    @CompileStatic
    @InheritConstructors
    public static class TargetNotFoundException extends RuntimeException { }
}
