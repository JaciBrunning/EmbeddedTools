package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.Artifact
import jaci.gradle.deploy.artifact.ArtifactDeployWorker
import jaci.gradle.deploy.artifact.BinaryLibraryArtifact
import jaci.gradle.deploy.artifact.NativeArtifact
import jaci.gradle.deploy.target.discovery.TargetDiscoveryWorker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.tasks.AbstractLinkTask
import org.gradle.platform.base.BinaryContainer

@CompileStatic
class DeployPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.getPluginManager().apply(ComponentModelBasePlugin)

        def deployExt = project.extensions.create("deploy", DeployExtension, project)

        deployExt.artifacts.withType(NativeArtifact).all { NativeArtifact art ->

        }

        project.gradle.buildFinished {
            TargetDiscoveryWorker.clearStorage()
            ArtifactDeployWorker.clearStorage()
        }
    }

    static class DeployRules extends RuleSource {
        @Mutate
        void createBinariesTasks(final ModelMap<Task> tasks, final ExtensionContainer ext, final BinaryContainer binaries) {
            def deployExtension = ext.getByType(DeployExtension)
            List<NativeArtifact> artifacts = []
            deployExtension.artifacts.withType(NativeArtifact).each { NativeArtifact artifact ->
                artifacts << artifact
            }
            artifacts.each { NativeArtifact artifact ->
                binaries.withType(NativeBinarySpec).each { NativeBinarySpec bin ->
                    if (artifact.appliesTo(bin)) {
                        bin.tasks.withType(AbstractLinkTask) { AbstractLinkTask task ->
                            artifact.dependsOn(task)
                        }

                        if (artifact.deployLibraries) {
                            deployExtension.artifacts.binaryLibraryArtifact("${artifact.name}Libraries") { BinaryLibraryArtifact bla ->
                                bla.binary = bin
                                artifact.configureLibsArtifact(bla)
                                bin.tasks.withType(AbstractLinkTask) { AbstractLinkTask task ->
                                    bla.dependsOn(task)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
