package jaci.gradle.deploy.target.discovery

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.log.ETLoggerFactory
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
import java.util.function.Consumer

@CompileStatic
class TargetDiscoveryTask extends DefaultTask implements Consumer<DeployContext> {

    @Internal
    final WorkerExecutor workerExecutor

    private DeployContext activeContext

    @Input
    RemoteTarget target

    @Inject
    TargetDiscoveryTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    public boolean available() {
        return activeContext != null
    }

    public DeployContext activeContext() {
        if (activeContext != null)
            return activeContext
        else
            throw new GradleException("Target ${target.name} is not available.")
    }

    @Override
    void accept(DeployContext ctx) {
        this.activeContext = ctx
    }

    @TaskAction
    void discoverTarget() {
        def log = ETLoggerFactory.INSTANCE.create("TargetDiscoveryTask[${target.name}]")

        log.log("Discovering Target ${target.name}")
        int hashcode = TargetDiscoveryWorker.submitStorage(target, this)

        // We use the Worker API since it allows for multiple of this task to run at the
        // same time. Inside the worker we split off into a threadpool so we can introduce
        // our own timeout logic.
        log.debug("Submitting worker ${hashcode}...")
        workerExecutor.submit(TargetDiscoveryWorker, { WorkerConfiguration config ->
            config.isolationMode = IsolationMode.NONE
            config.params hashcode
        } as Action)
        log.debug("Submitted!")
    }
}
