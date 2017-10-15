package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.tasks.DeployerDeployConfigureTask
import jaci.gradle.deploy.tasks.MasterDeployTask
import jaci.gradle.deploy.tasks.TargetDeployConfigureTask
import jaci.gradle.deploy.tasks.TargetDeployTask
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
            def discover = project.tasks.create("discover${target.name.capitalize()}".toString(), TargetDiscoveryTask) { TargetDiscoveryTask task ->
                task.group = "EmbeddedTools"
                task.description = "Determine the address(es) of target ${target.name.capitalize()}"
                task.target = target
            }

            def runDeploy = project.tasks.create("runDeploy${target.name.capitalize()}", TargetDeployTask) { TargetDeployTask task ->
                task.deployers = this.deployers
                task.target = target
                task.dependsOn discover
            }

            project.tasks.create("deploy${target.name.capitalize()}", TargetDeployConfigureTask) { TargetDeployConfigureTask task ->
                task.group = "EmbeddedTools"
                task.description = "Deploy all deployers for ${target.name.capitalize()} target"
                task.deployers = this.deployers
                task.target = target
                task.finalizedBy runDeploy
            }
        }

        this.deployers.all { Deployer deployer ->
            deployer.project = project

            project.tasks.create("deploy${deployer.name.capitalize()}", DeployerDeployConfigureTask) { DeployerDeployConfigureTask task ->
                task.group = "EmbeddedTools"
                task.description = "Deploy the ${deployer.name.capitalize()} deployer on all targets"
                task.deployer = deployer

                project.afterEvaluate {
                    project.tasks.withType(TargetDeployTask).all { TargetDeployTask runDeployTask ->
                        if (deployer.targets.contains(runDeployTask.target.name))
                            task.finalizedBy(runDeployTask)
                    }
                }
            }
        }

        project.tasks.create("deploy", MasterDeployTask) { MasterDeployTask task ->
            task.group = "EmbeddedTools"
            task.description = "Deploy all deployers for all targets"
            task.deployers = this.deployers

            project.afterEvaluate {
                project.tasks.withType(TargetDeployTask).all { TargetDeployTask runDeployTask ->
                    task.finalizedBy(runDeployTask)
                }
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
