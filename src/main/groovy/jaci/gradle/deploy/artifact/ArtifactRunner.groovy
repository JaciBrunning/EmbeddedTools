package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.context.DeployContext

@CompileStatic
class ArtifactRunner {

    static void runDeploy(Artifact artifact, DeployContext context) {
        artifact.getPredeploy()?.each { Closure c -> ClosureUtils.delegateCall(context, c) }
        artifact.deploy(context)
        artifact.getPostdeploy()?.each { Closure c -> ClosureUtils.delegateCall(context, c) }
    }

}
