package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class TargetDeployConfigureTask extends DefaultTask {
    @Input
    RemoteTarget target

    @Input
    NamedDomainObjectContainer<Deployer> deployers

    @TaskAction
    void configureDeployer() {
        deployers.all { Deployer dep ->
            if (dep.targets.contains(target.name)) dep._active << target.name
        }
    }
}
