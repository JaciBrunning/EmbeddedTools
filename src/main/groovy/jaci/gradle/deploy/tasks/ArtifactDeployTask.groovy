package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.WorkerStorage
import jaci.gradle.deploy.artifact.AbstractArtifact
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
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

    private static class DeployStorage {
        Project project
        DeployContext ctx
        AbstractArtifact artifact

        DeployStorage(Project project, DeployContext ctx, AbstractArtifact artifact) {
            this.project = project
            this.ctx = ctx
            this.artifact = artifact
        }
    }

    // TODO: clear deployerstorage on build finished
    @Internal
    final WorkerExecutor workerExecutor
    @Internal
    private static WorkerStorage<DeployStorage> deployerStorage  = WorkerStorage.obtain()

    public static void clearStorage() {
        deployerStorage.clear()
    }

    @Input
    AbstractArtifact artifact

    @Inject
    ArtifactDeployTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void deployArtifact() {
        def discoveries = dependsOn.findAll {
            i -> i instanceof TargetDiscoveryTask && (i as TargetDiscoveryTask).isTargetActive()
        }.collect {
            it as TargetDiscoveryTask
        }

//        artifact.taskDependencies = taskDependencies.getDependencies(this) as Set<Task>

        discoveries.each { TargetDiscoveryTask discover ->
            def index = deployerStorage.put(new DeployStorage(project, discover.getContext(), artifact))
            workerExecutor.submit(DeployArtifactWorker, ({ WorkerConfiguration config ->
                config.isolationMode = IsolationMode.NONE
                config.params index
            } as Action))
        }
    }

    static class DeployArtifactWorker implements Runnable {
        int index

        @Inject
        DeployArtifactWorker(Integer index) {
            this.index = index
        }

        @Override
        void run() {
            def storage = deployerStorage.get(index)
//            storage.artifact.doDeploy(storage.project, storage.ctx)
        }
    }
}
