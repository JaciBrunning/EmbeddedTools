package jaci.gradle.deploy

import jaci.gradle.deploy.artifact.ArtifactsExtension
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.TargetsExtension
import jaci.gradle.deploy.tasks.TargetDiscoveryTask
import org.gradle.api.Project

class DeployExtension {
    TargetsExtension targets
    ArtifactsExtension artifacts

    Project project

    DeployExtension(Project project) {
        this.project = project
        targets = new TargetsExtension(project)
        artifacts = new ArtifactsExtension(project)

        this.targets.all { RemoteTarget target ->
            // Discover the Remote Target on the network
            def discover = project.tasks.create("discover${target.name.capitalize()}".toString(), TargetDiscoveryTask) { TargetDiscoveryTask task ->
                task.group = "EmbeddedTools"
                task.description = "Determine the address(es) of target ${target.name.capitalize()}"
                task.target = target
            }

//            def deploy = project.tasks.create("deploy${target.name.capitalize()}".toString(), TargetDeployTask) { TargetDeployTask task ->
//                task.group = "EmbeddedTools"
//                task.description = "Deploy to target ${target.name.capitalize()}"
//                task.dependsOn discover
//            }
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

    def targets(final Closure closure) {
        project.configure(targets as Object, closure)
    }

    def artifacts(final Closure closure) {
        project.configure(artifacts as Object, closure)
    }
}
