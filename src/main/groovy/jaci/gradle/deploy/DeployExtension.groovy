package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.AbstractArtifact
import jaci.gradle.deploy.artifact.ArtifactsExtension
import jaci.gradle.deploy.artifact.CacheableArtifact
import jaci.gradle.deploy.cache.CacheExtension
import jaci.gradle.deploy.discovery.TargetDiscoveryTask
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.TargetsExtension
import jaci.gradle.deploy.tasks.ArtifactDeployTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task

import javax.inject.Inject
import java.util.concurrent.Callable

@CompileStatic
class DeployExtension {
    TargetsExtension targets
    ArtifactsExtension artifacts
    CacheExtension cache

    Project project

    @Inject
    DeployExtension(Project project) {
        this.project = project
        targets = new TargetsExtension(project)
        artifacts = new ArtifactsExtension(project)
        cache = new CacheExtension(project)

        this.targets.all { RemoteTarget target ->
            // Discover the Remote Target on the network
            project.tasks.create("discover${target.name.capitalize()}".toString(), TargetDiscoveryTask) { TargetDiscoveryTask task ->
                task.group = "EmbeddedTools"
                task.description = "Determine the address(es) of target ${target.name.capitalize()}"
                task.target = target
            }
        }

        this.artifacts.all { AbstractArtifact artifact ->
            if (artifact instanceof CacheableArtifact)
                ((CacheableArtifact)artifact).setCacheResolver(this.cache)

            artifact.targets.all { Object tObj ->
                RemoteTarget target = this.targets.resolve(tObj)
                project.tasks.create("deploy${artifact.name.capitalize()}${target.name.capitalize()}".toString(), ArtifactDeployTask) { ArtifactDeployTask task ->
                    task.artifact = artifact
                    task.target = target

                    task.dependsOn({ project.tasks.withType(TargetDiscoveryTask).findAll { TargetDiscoveryTask t -> t.target == target }} as Callable<List<Task>> )
                    task.dependsOn(artifact.dependencies)
                }
            }
//
//            project.tasks.create("deploy${artifact.name.capitalize()}".toString(), ArtifactDeployTask) { ArtifactDeployTask task ->
//                task.artifact = artifact
//                project.tasks.withType(TargetDiscoveryTask).all { TargetDiscoveryTask task2 ->
//                    artifact.targets.matching { it == task2.target.name }.all { String s ->
//                        task.dependsOn(task2)
//                    }
//                }
//
//                artifact.dependencies.all { Object dep ->
//                    if (dep instanceof Closure) {
//                        task.dependsOn(dep.call(project))
//                    } else if (dep instanceof Action) {
//                        task.dependsOn(dep.execute(project))
//                    } else {
//                        task.dependsOn(dep)
//                    }
//                }
//            }
        }

        project.tasks.create("deploy") { Task task ->
            task.group = "EmbeddedTools"
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

    def cache(final Closure closure) {
        project.configure(cache as Object, closure)
    }
}
