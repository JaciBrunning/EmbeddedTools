package jaci.gradle.deploy

import jaci.gradle.deploy.artifact.ArtifactBase
import jaci.gradle.deploy.artifact.ArtifactsExtension
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.TargetsExtension
import jaci.gradle.deploy.tasks.ArtifactDeployTask
import jaci.gradle.deploy.tasks.TargetDiscoveryTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task

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
            project.tasks.create("discover${target.name.capitalize()}".toString(), TargetDiscoveryTask) { TargetDiscoveryTask task ->
                task.group = "EmbeddedTools"
                task.description = "Determine the address(es) of target ${target.name.capitalize()}"
                task.target = target
            }
        }

        this.artifacts.all { ArtifactBase artifact ->
            project.tasks.create("deploy${artifact.name.capitalize()}".toString(), ArtifactDeployTask) { ArtifactDeployTask task ->
                task.artifact = artifact
                project.tasks.withType(TargetDiscoveryTask).all { TargetDiscoveryTask task2 ->
                    artifact.targets.matching { it == task2.target.name }.all { String s ->
                        task.dependsOn(task2)
                    }
                }

                artifact.dependencies.all { Object dep ->
                    if (dep instanceof Closure) {
                        task.dependsOn(dep.call(project))
                    } else if (dep instanceof Action) {
                        task.dependsOn(dep.execute(project))
                    } else {
                        task.dependsOn(dep)
                    }
                }
            }
        }

        project.tasks.create("deploy") { Task task ->
            task.group = "GradleRIO"
            task.description = "Deploy all artifacts on all targets"
            project.tasks.withType(ArtifactDeployTask).all { ArtifactDeployTask task2 ->
                task.dependsOn(task2)
            }
        }
    }

    def targets(final Closure closure) {
        project.configure(targets as Object, closure)
    }

    def artifacts(final Closure closure) {
        project.configure(artifacts as Object, closure)
    }
}
