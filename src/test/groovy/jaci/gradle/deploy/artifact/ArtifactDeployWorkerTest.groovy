package jaci.gradle.deploy.artifact

import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.log.ETLogger
import spock.lang.Specification

class ArtifactDeployWorkerTest extends Specification {

    def subctx = Mock(DeployContext) {
        getLogger() >> Mock(ETLogger)
    }
    def context = Mock(DeployContext) {
        subContext(_) >> subctx
    }
    def enabledArtifact = Mock(Artifact) {
        isEnabled(_) >> true
    }
    def disabledArtifact = Mock(Artifact) {
        isEnabled(_) >> false
    }

    def "runs deploy enabled"() {
        def worker = new ArtifactDeployWorker(context, enabledArtifact)

        when:
        worker.run()

        // We should only call runDeploy with the correct subcontext
        then:
        1 * enabledArtifact.deploy(subctx)
        0 * enabledArtifact.deploy(_)
    }

    def "runs skipped disabled"() {
        def worker = new ArtifactDeployWorker(context, disabledArtifact)

        when:
        worker.run()

        then:
        0 * disabledArtifact.deploy(_)
    }

    def "storage"() {
        ArtifactDeployWorker.clearStorage()

        // Check that it gets inserted
        when:
        def hc = ArtifactDeployWorker.submitStorage(context, enabledArtifact)
        then:
        ArtifactDeployWorker.storageCount() == 1

        // Check that, after construction, it is removed from that map
        // and its attributes match
        when:
        def worker = new ArtifactDeployWorker(hc)
        then:
        ArtifactDeployWorker.storageCount() == 0
        worker.ctx == context
        worker.artifact == enabledArtifact
    }

}
