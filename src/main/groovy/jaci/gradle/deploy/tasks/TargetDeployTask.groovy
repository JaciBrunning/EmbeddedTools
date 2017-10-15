package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DefaultDeployContext
import jaci.gradle.deploy.DeployLogger
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.hidetake.groovy.ssh.connection.AllowAnyHosts

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

        def log = new DeployLogger(0)
        log.log("-> Target ${target.name}")

        def activeDeployers = deployers.findAll { Deployer dep -> dep._active.contains(target.name) }
        if (activeDeployers.size() > 0) {
            EmbeddedTools.ssh.run {
                session(host: target._active_address, user: target.user, password: target.password, timeoutSec: target.timeout, knownHosts: AllowAnyHosts.instance) {
                    def ctx = new DefaultDeployContext(target, log, delegate, target.directory ?: '.')
                    activeDeployers.each { Deployer dep ->
                        dep.doDeploy(ctx)
                    }
                }
            }
        }
    }
}
