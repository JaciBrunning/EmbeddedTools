package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.ArtifactBase
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.deployer.NativeArtifact
import jaci.gradle.deploy.deployer.NativeLibraryArtifact
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.nativedeps.NativeLibBinary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.*
import org.gradle.nativeplatform.tasks.AbstractLinkTask
import org.gradle.platform.base.BinaryContainer

@CompileStatic
class DeployPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def targets = project.container(RemoteTarget)
        def deployers = project.container(Deployer)

        def deployExt = new DeployExtension(project, targets, deployers)
        project.extensions.add('deploy', deployExt)
    }

    static class DeployRules extends RuleSource {
        @Mutate
        void createBinariesTasks(final ModelMap<Task> tasks, final Repositories repos, final ExtensionContainer ext, final BinaryContainer binaries) {
            ext.getByType(DeployExtension).deployers.each { Deployer deployer ->
                deployer.artifacts.each { ArtifactBase artifact ->
                    if (artifact instanceof NativeArtifact) {
                        NativeArtifact na = artifact as NativeArtifact
                        binaries.each { bin ->
                            if (bin instanceof NativeBinarySpec) {
                                NativeBinarySpec spec = bin as NativeBinarySpec

                                if (spec.component.name == na.component && spec.targetPlatform.name == na.targetPlatform) {
                                    spec.tasks.withType(AbstractLinkTask) { AbstractLinkTask task ->
                                        na.linkOut = task.outputs
                                    }
                                }
                            }
                        }
                    } else if (artifact instanceof NativeLibraryArtifact) {
                        NativeLibraryArtifact nla = artifact as NativeLibraryArtifact
                        repos.withType(PrebuiltLibraries).all { PrebuiltLibraries repo ->
                            repo.matching { PrebuiltLibrary pl -> pl.name == nla.library }.all { PrebuiltLibrary pl ->
                                pl.binaries.all { NativeLibraryBinary bin ->
                                    FileCollection sharedLibs = (bin instanceof NativeLibBinary) ? (bin as NativeLibBinary).runtimeLibraries : bin.runtimeFiles
                                    FileTree deployedFileTree = sharedLibs.asFileTree
                                    if (nla.matchers != null && !nla.matchers.empty) {
                                        deployedFileTree = deployedFileTree.matching { PatternFilterable pat -> pat.include(nla.matchers) }
                                    }

                                    nla.files = deployedFileTree
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
