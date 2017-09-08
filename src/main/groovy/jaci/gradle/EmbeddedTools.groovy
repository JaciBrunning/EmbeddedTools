package jaci.gradle
import jaci.gradle.*

import org.gradle.api.*
import org.gradle.model.*
import groovy.util.*

class DeployTools implements Plugin<Project> {
    void apply(Project project) {
        
    }

    static class DeployRules extends RuleSource {
        @Model("targets")
        void createTargetsModel(TargetsSpec spec) { }

        @Defaults 
        void setDefaultTargetValues(@Each RemoteTarget target) {
            target.setAddresses([])
            target.setPromptPassword(false)
            target.setTimeout(5)
            target.setFailOnMissing(true)
        }

        @Model("deployers")
        void createDeployersModel(DeployersSpec spec) { }

        @Defaults
        void setDefaultDeployerValues(@Each Deployer deployer) {
            deployer.setOrder(50)
        }

        @Defaults
        void setDefaultArtifactValues(@Each ArtifactBase artifact) {
            artifact.setOrder(50)
        }

        @Defaults
        void setDefaultFileArtifactValues(@Each FileArtifact artifact) {
            artifact.setEnableCache(true)
        }

        @Mutate
        void createDeployerTasks(final ModelMap<Task> tasks, final DeployersSpec deployers, final TargetsSpec targets) {
            targets.forEach { target ->
                tasks.create('deployTarget' + target.name.capitalize(), DeployTargetTask) { task ->
                    task.target = target
                }
            }

            deployers.forEach { deployer ->
                tasks.create('deploy' + deployer.name.capitalize(), DeployerTask) { task ->
                    task.group 'DeployTools'
                    task.description "Run ${deployer.name} deployer for all artifacts"

                    task.deployer = deployer

                    targets.findAll { target -> target.name in deployer.targets }.forEach { target ->
                        task.finalizedBy('deployTarget' + target.name.capitalize())
                    }
                }
            }

            tasks.create('deploy', DefaultTask) { deployTask -> 
                deployTask.group "DeployTools"
                deployTask.description "Run all registered deployers"

                deployers.forEach { deployer ->
                    deployTask.dependsOn('deploy' + deployer.name.capitalize())
                }
            }
        }
    }
}