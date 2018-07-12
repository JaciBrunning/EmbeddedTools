package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.deploy.artifact.Artifact
import jaci.gradle.deploy.artifact.NativeArtifact
import jaci.gradle.deploy.target.discovery.TargetDiscoveryWorker
import jaci.gradle.deploy.artifact.ArtifactDeployWorker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
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
        def deployExt = project.extensions.create("deploy", DeployExtension, project)

        project.gradle.buildFinished {
            TargetDiscoveryWorker.clearStorage()
            ArtifactDeployWorker.clearStorage()
        }
    }

    static class DeployRules extends RuleSource {
        @Mutate
        void createBinariesTasks(final ModelMap<Task> tasks, final ExtensionContainer ext, final BinaryContainer binaries) {
            ext.getByType(DeployExtension).artifacts.each { Artifact artifact ->
                if (artifact instanceof NativeArtifact) {
                    NativeArtifact na = artifact as NativeArtifact
                    binaries.each { bin ->
                        if (bin instanceof NativeBinarySpec) {
                            NativeBinarySpec spec = bin as NativeBinarySpec

                            if (spec.component.name == na.component && spec.targetPlatform.name == na.targetPlatform) {
                                spec.tasks.withType(AbstractLinkTask) { AbstractLinkTask task ->
                                    na.dependsOn(task)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
