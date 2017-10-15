package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction

@CompileStatic
class TargetDeployTask extends DefaultTask {
    @Input
    RemoteTarget target

    @Input
    NamedDomainObjectContainer<Deployer> deployers

    @TaskAction
    void deploy() {
        if (target._active_address == null) {
            throw new StopExecutionException()
        }

        def activeDeployers = deployers.findAll { Deployer dep -> dep._active.contains(target.name) }
        println "Deploying for ${target}"
        activeDeployers.each {
            println it
        }
        println ""
    }
}
