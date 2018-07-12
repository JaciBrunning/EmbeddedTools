package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.CommandDeployResult
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project

@CompileStatic
class CommandArtifact extends AbstractArtifact {

    CommandArtifact(Project project, String name) {
        super(project, name)
    }

    String command = null
    CommandDeployResult result = null

    @Override
    void deploy(DeployContext context) {
        result = context.execute(command)
    }

}
