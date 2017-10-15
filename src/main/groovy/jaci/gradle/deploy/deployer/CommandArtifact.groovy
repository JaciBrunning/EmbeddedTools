package jaci.gradle.deploy.deployer

import jaci.gradle.deploy.DeployContext

class CommandArtifact extends ArtifactBase {
    CommandArtifact(String name) {
        super(name)
    }

    String command = null

    @Override
    void deploy(DeployContext ctx) {
        ctx.runCommand(command)
    }
}
