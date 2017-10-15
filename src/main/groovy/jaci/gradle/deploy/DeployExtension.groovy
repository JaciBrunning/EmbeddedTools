package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.tasks.TargetDiscoveryTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

@CompileStatic
class DeployExtension {
    NamedDomainObjectContainer<RemoteTarget> targets
    NamedDomainObjectContainer<Deployer> deployers

    DeployExtension(Project project, NamedDomainObjectContainer<RemoteTarget> targets, NamedDomainObjectContainer<Deployer> deployers) {
        this.targets = targets
        this.deployers = deployers

        this.deployers.all { Deployer deployer ->
            deployer.project = project
        }

        this.targets.all { RemoteTarget target ->
            project.tasks.create("discover${target.name.capitalize()}", TargetDiscoveryTask) { TargetDiscoveryTask task ->
                task.group = "EmbeddedTools"
                task.description = "Determine the address(es) of target ${target.name.capitalize()}"
                task.target = target
            }
        }
    }

    def targets(final Closure config) {
        targets.configure(config)
    }

    def deployers(final Closure config) {
        deployers.configure(config)
    }
}
