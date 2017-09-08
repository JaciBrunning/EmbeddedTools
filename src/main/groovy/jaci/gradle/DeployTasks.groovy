package jaci.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class DeployerTask extends DefaultTask {
    @Input
    Deployer deployer

    @TaskAction
    void configureDeploy() {
        ext.deployer = deployer
    }
}

class DeployTargetTask extends DefaultTask {
    @Input
    RemoteTarget target

    @TaskAction
    void deploy() {
        // TODO: Determine target address

        def deployerList = []
        // Find all deployers that will be bound to this target
        project.tasks.findAll { this in it.finalizedBy.getDependencies() }.forEach { deployerTask -> 
            deployerList << deployerTask.ext.deployer
        }
        deployerList = deployerList.toSorted { a, b -> a.getOrder() <=> b.getOrder() }

        // Start SSH session
        println "<<< CONNECT ${target} >>>"
        deployerList.forEach { deployer -> 
            println "\t<< DEPLOY ${deployer} >>"
            deployer.getPredeploy().forEach { cmd ->
                println "\t\tCMD > ${cmd}"
            } 

            deployer.getArtifacts().toSorted { a, b -> a.getOrder() <=> b.getOrder() }.forEach { artifact ->
                println "\t\tART > ${artifact}"
                if (artifact.getPredeploy() != null) artifact.getPredeploy().forEach { cmd ->
                    println "\t\t\tCMD > ${cmd}"
                }

                if (artifact instanceof JavaArtifact) {
                    def task = project.tasks.findByName(artifact.name)
                    if (task != null) {
                        def files = task.outputs.files.files
                        
                    }
                } else if (artifact instanceof NativeArtifact) {

                } else if (artifact instanceof FileArtifact) {

                } else if (artifact instanceof CommandArtifact) {
                    
                }
                
                if (artifact.getPostdeploy() != null) artifact.getPostdeploy().forEach { cmd ->
                    println "\t\t\tCMD > ${cmd}"
                }
            }

            deployer.getPostdeploy().forEach { cmd -> 
                println "\t\tCMD > ${cmd}"
            }
        }
    }
}