package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.Deployer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class DeployerDeployConfigureTask extends DefaultTask {
    @Input
    Deployer deployer

    @TaskAction
    void configureDeployer() {
        deployer._active = deployer.targets
    }
}
