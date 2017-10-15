package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic

@CompileStatic
abstract class ArtifactBase extends DeployableStep {
    ArtifactBase(String name) {
        super(name)
    }
}
