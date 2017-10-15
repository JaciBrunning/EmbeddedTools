package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.Deployer
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class MasterDeployTask extends DefaultTask {
    @Input
    NamedDomainObjectContainer<Deployer> deployers

    @TaskAction
    void configureDeployers() {
        deployers.all { Deployer dep ->
            dep._active = dep.targets
        }
    }
}
