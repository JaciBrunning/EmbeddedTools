package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext

@CompileStatic
class CommandArtifact extends ArtifactBase {
    CommandArtifact(String name) {
        super(name)
    }

    String command = null
    boolean ignoreError = false

    String result = null

    @Override
    void deploy(DeployContext ctx) {
        if (ignoreError)
            result = ctx.executeMaybe(command)
        else
            result = ctx.execute(command)
    }
}
