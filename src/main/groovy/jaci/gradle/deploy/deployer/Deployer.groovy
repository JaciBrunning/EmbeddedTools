package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import org.gradle.api.Project

@CompileStatic
class Deployer extends DeployableStep {

    Deployer(String name) {
        super(name)
    }

    Project project

    List<String> targets    = []
    List<ArtifactBase> artifacts = []

    // Calculated Values (from tasks)
    List<String> _active = []       // Active targets

    def artifact(String name, Class<? extends ArtifactBase> type, final Closure config) {
        def artifact = type.newInstance(name)
        project.configure(artifact, config)
        artifacts.add(artifact)
    }

    def fileArtifact(String name, final Closure config) {
        artifact(name, FileArtifact, config)
    }

    def fileCollectionArtifact(String name, final Closure config) {
        artifact(name, FileCollectionArtifact, config)
    }

    def commandArtifact(String name, final Closure config) {
        artifact(name, CommandArtifact, config)
    }

    @Override
    void deploy(DeployContext ctx) {
        artifacts.each { artifact -> artifact.doDeploy(ctx) }
    }
}
