package jaci.gradle.deploy.tasks

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jaci.gradle.RefcountList
import jaci.gradle.deploy.artifact.Artifact
import jaci.gradle.deploy.sessions.context.DeployContext

import javax.inject.Inject

@CompileStatic
class ArtifactDeployWorker implements Runnable {

    // Static Storage for Workers
    // In Gradle, we can't run a task in parallel using workers and pass non-serializable data
    // to the worker. To get around this, we store them statically and clear them at the conclusion
    // of the build. It's not at all advised, but it's the best we've got.

    @Canonical
    @CompileStatic
    private static class DeployStorage {
        DeployContext ctx
        Artifact artifact
    }

    private static RefcountList<DeployStorage> deployerStorage = new RefcountList<>()

    public static void clearStorage() {
        deployerStorage.clear()
    }

    public static int submitStorage(DeployContext context, Artifact artifact) {
        return deployerStorage.put(new DeployStorage(context, artifact))
    }

    // Begin worker

    int index

    @Inject
    ArtifactDeployWorker(Integer index) {
        this.index = index
    }

    @Override
    void run() {
        def storage = deployerStorage.get(index)
        deploy(storage.ctx, storage.artifact)
    }

    void deploy(DeployContext ctx, Artifact artifact) {
        def context = ctx.subContext(artifact.getDirectory())

        if (artifact.isEnabled(context)) {
            artifact.runDeploy(context)
        } else {
            context.logger.log("Artifact skipped...")
            artifact.runSkipped(context)
        }
    }
}
