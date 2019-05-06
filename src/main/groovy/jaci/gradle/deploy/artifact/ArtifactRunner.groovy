package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext

@CompileStatic
class ArtifactRunner {

    static void runDeploy(Artifact artifact, DeployContext context) {
        artifact.predeploy?.each {it.execute(context)}
        artifact.deploy(context)
        artifact.postdeploy?.each {it.execute(context)}
    }

}
