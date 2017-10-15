package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class DeployPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def targets = project.container(RemoteTarget)
        def deployers = project.container(Deployer)

        def deployExt = new DeployExtension(project, targets, deployers)
        project.extensions.add('deploy', deployExt)
    }
}
