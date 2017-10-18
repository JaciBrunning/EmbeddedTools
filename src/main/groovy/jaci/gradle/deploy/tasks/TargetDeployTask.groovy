package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class TargetDeployTask extends DefaultTask {

    DeployContext context

    @TaskAction
    void deployTarget() {

    }

}
