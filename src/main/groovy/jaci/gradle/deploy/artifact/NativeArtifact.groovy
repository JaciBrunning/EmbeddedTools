package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.nativeplatform.tasks.AbstractLinkTask

@CompileStatic
class NativeArtifact extends FileArtifact implements TaskHungryArtifact {
    NativeArtifact(String name) {
        super(name)
        component = name
    }

    String component = null
    String targetPlatform = null

    @Override
    void taskDependenciesAvailable(Set<Task> tasks) {
        def nativefile = tasks.findAll { it instanceof AbstractLinkTask }.collect { Task t ->
            t.outputs.files.files.findAll { File f -> f.isFile() }.first()
        }.first()
        file.set(nativefile)
    }
}
