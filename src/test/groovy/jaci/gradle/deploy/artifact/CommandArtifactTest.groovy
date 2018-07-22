package jaci.gradle.deploy.artifact

import jaci.gradle.deploy.CommandDeployResult
import jaci.gradle.deploy.context.DeployContext

class CommandArtifactTest extends AbstractArtifactTestSpec {

    CommandArtifact artifact

    def setup() {
        artifact = new CommandArtifact(name, project)
    }

    def "deploy"() {
        def result = Mock(CommandDeployResult)
        def ctx = Mock(DeployContext) {
            execute(_) >> result
        }
        def cmd = "Hello World"
        artifact.command = cmd

        when:
        artifact.deploy(ctx)
        then:
        artifact.result == result
    }
}
