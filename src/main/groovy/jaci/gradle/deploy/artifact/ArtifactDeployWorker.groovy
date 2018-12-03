package jaci.gradle.deploy.artifact

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import jaci.gradle.deploy.context.DeployContext

import javax.inject.Inject

@CompileStatic
class ArtifactDeployWorker implements Runnable {

    // Static Storage for Workers
    // In Gradle, we can't run a task in parallel using workers and pass non-serializable data
    // to the worker. To get around this, we store them statically and clear them at the conclusion
    // of the build. It's not at all advised, but it's the best we've got.

    @TupleConstructor
    @EqualsAndHashCode
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
        def hash = ds.hashCode()
        deployerStorage.put(hash, ds)
        return hash
    }

    public static int storageCount() {
        return deployerStorage.size()
    }

    // Begin worker

    DeployContext ctx
    Artifact artifact

    ArtifactDeployWorker(DeployContext ctx, Artifact artifact) {
        this.ctx = ctx
        this.artifact = artifact
    }

    ArtifactDeployWorker(DeployStorage storage) {
        this(storage.ctx, storage.artifact)
    }

    @Inject
    ArtifactDeployWorker(Integer hashCode) {
        this(deployerStorage.get(hashCode))
        deployerStorage.remove(hashCode)
    }

    @Override
    void run() {
        def context = ctx.subContext(artifact.getDirectory())
        def enabled = artifact.isEnabled(context)

        if (enabled) {
            ArtifactRunner.runDeploy(artifact, context)
        } else {
            context.logger.log("Artifact skipped...")
        }
    }
}
