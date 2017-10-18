package jaci.gradle.deploy

import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.tasks.TargetDiscoveryTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class DeployExtension {
    NamedDomainObjectContainer<RemoteTarget> targets
    NamedDomainObjectContainer<Deployer> deployers

    DeployExtension(Project project, NamedDomainObjectContainer<RemoteTarget> targets, NamedDomainObjectContainer<Deployer> deployers) {
        this.targets = targets
        this.deployers = deployers

        this.targets.all { RemoteTarget target ->
            // Discover the Remote Target on the network
            def discover = project.tasks.create("discover${target.name.capitalize()}".toString(), TargetDiscoveryTask) { TargetDiscoveryTask task ->
                task.group = "EmbeddedTools"
                task.description = "Determine the address(es) of target ${target.name.capitalize()}"
                task.target = target
            }
        }

        this.deployers.all { Deployer deployer ->
            deployer.project = project
        }

        // Configures all deployers as active.
        // Runs runDeploy after configuration for all targets
//        project.tasks.create("deploy", MasterDeployTask) { MasterDeployTask task ->
//            task.group = "EmbeddedTools"
//            task.description = "Deploy all deployers for all targets"
//            task.deployers = this.deployers
//
//            project.afterEvaluate {
//                project.tasks.withType(TargetDeployTask).all { TargetDeployTask runDeployTask ->
//                    task.finalizedBy(runDeployTask)
//                }
//            }
//        }
    }

    def targets(final Closure config) {
        targets.configure(config)
    }

    def deployers(final Closure config) {
        deployers.configure(config)
    }
}
