package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.discovery.TargetDiscoveryTask
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

@CompileStatic
class ArtifactDeployTask extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @Input
    Artifact artifact
    @Input
    RemoteTarget target

    @Inject
    ArtifactDeployTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void deployArtifact() {
        Set<Task> deps = taskDependencies.getDependencies(this) as Set<Task>

        if (artifact instanceof TaskHungryArtifact)
            ((TaskHungryArtifact)artifact).taskDependenciesAvailable(deps)

        def discoveries = deps.findAll { i ->
            i instanceof TargetDiscoveryTask &&
            ((TargetDiscoveryTask)i).available() &&
            ((TargetDiscoveryTask)i).target.equals(target)
        }.collect { it as TargetDiscoveryTask }

        discoveries.each { TargetDiscoveryTask discover ->
            def index = ArtifactDeployWorker.submitStorage(discover.activeContext(), artifact)
            workerExecutor.submit(ArtifactDeployWorker, ({ WorkerConfiguration config ->
                config.isolationMode = IsolationMode.NONE
                config.params index
            } as Action))
        }
    }
}
