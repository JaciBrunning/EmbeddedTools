package jaci.gradle.deploy.artifact

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext

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

    private static Map<Integer, DeployStorage> deployerStorage = new HashMap<>()

    public static void clearStorage() {
        deployerStorage.clear()
    }

    public static int submitStorage(DeployContext context, Artifact artifact) {
        def ds = new DeployStorage(context, artifact)
        deployerStorage.put(ds.hashCode(), ds)
        return ds.hashCode()
    }

    public static void removeStorage(int hashcode) {
        deployerStorage.remove(hashcode)
    }

    // Begin worker

    int hashCode

    @Inject
    ArtifactDeployWorker(Integer hashCode) {
        this.hashCode = hashCode
    }

    @Override
    void run() {
        def storage = deployerStorage.get(hashCode)
        try {
            deploy(storage.ctx, storage.artifact)
        } finally {
            removeStorage(hashCode)
        }
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
