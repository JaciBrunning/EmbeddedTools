package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.deployer.ArtifactBase
import jaci.gradle.deploy.deployer.Deployer
import jaci.gradle.deploy.deployer.NativeArtifact
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
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
        void createBinariesTasks(final ModelMap<Task> tasks, ExtensionContainer ext, BinaryContainer binaries) {
            ext.getByType(DeployExtension).deployers.each { Deployer deployer ->
                deployer.artifacts.each { ArtifactBase artifact ->
                    if (artifact instanceof NativeArtifact) {
                        NativeArtifact na = artifact as NativeArtifact
                        binaries.each { bin ->
                            if (bin instanceof NativeBinarySpec) {
                                NativeBinarySpec spec = bin as NativeBinarySpec
                                
                                if (spec.component.name == na.component && spec.targetPlatform.name == na.targetPlatform) {
                                    if (spec instanceof NativeExecutableBinarySpec) {
                                        na.file = (spec as NativeExecutableBinarySpec).executable.file
                                    } else if (spec instanceof SharedLibraryBinarySpec) {
                                        na.file = (spec as SharedLibraryBinarySpec).sharedLibraryFile
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
