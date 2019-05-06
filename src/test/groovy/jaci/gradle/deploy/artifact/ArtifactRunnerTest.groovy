package jaci.gradle.deploy.artifact

import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Action
import spock.lang.Specification
import spock.lang.Subject

class ArtifactRunnerTest extends Specification {

    def "runs in order"() {
        def predep = [Mock(Action)]
        def postdep = [Mock(Action)]
        def context = Mock(DeployContext)

        def artifact = Mock(Artifact) {
            getPredeploy() >> predep
            getPostdeploy() >> postdep
        }

        when:
        ArtifactRunner.runDeploy(artifact, context)
        then:
        1 * predep.last().execute(context)
        then:
        1 * artifact.deploy(context)
        then:
        1 * postdep.last().execute(context)
    }

}
