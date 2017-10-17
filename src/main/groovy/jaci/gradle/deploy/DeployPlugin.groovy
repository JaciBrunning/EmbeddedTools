package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.ArtifactBase
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.deployer.NativeArtifact
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.nativedeps.NativeLibBinary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.language.nativeplatform.DependentSourceSet
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

//                                if (na.libraries) {
//                                    bin.inputs.withType(DependentSourceSet) { DependentSourceSet ss ->
//                                        ss.libs.each { lss ->
//                                            if (lss instanceof LinkedHashMap) {
//                                                def lib = lss['library'] as String
////                                                def nl = (repos.getByName('embeddedTools') as PrebuiltLibraries).getByName(lib).binaries.first()
//                                                repos.matching { ArtifactRepository repo -> repo.name == 'embeddedTools' }.all { PrebuiltLibraries repo ->
//                                                    (repo as PrebuiltLibraries).matching { PrebuiltLibrary pl -> pl.name == lib }.all { PrebuiltLibrary pl ->
//                                                        pl.binaries.whenObjectAdded { NativeLibraryBinary nl ->
//                                                            if (nl instanceof NativeLibBinary) {
//                                                                def natLib = nl as NativeLibBinary
//                                                                na.libraryFiles = (na.libraryFiles == null ? natLib.runtimeFiles : na.libraryFiles + natLib.runtimeFiles)
//                                                            }
//                                                        }
//                                                    }
//                                                }
////                                                if (nl instanceof NativeLibBinary) {
////                                                    def natLib = nl as NativeLibBinary
////                                                    na.libraryFiles = (na.libraryFiles == null ? natLib.runtimeFiles : na.libraryFiles + natLib.runtimeFiles)
////                                                }
//                                            }
//                                        }
//                                    }
//                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
