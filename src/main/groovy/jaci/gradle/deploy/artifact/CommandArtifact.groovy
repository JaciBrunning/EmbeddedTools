package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext

@CompileStatic
class CommandArtifact extends AbstractArtifact {

    CommandArtifact(String name) {
        super(name)
    }

    String command = null

    String result = null

    @Override
    void deploy(DeployContext context) {
        result = context.execute(command)
    }
}
