package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.nativeplatform.tasks.AbstractLinkTask

@CompileStatic
class NativeArtifact extends FileArtifact implements TaskHungryArtifact {

    NativeArtifact(String name, Project project) {
        super(name, project)
        component = name
    }

    // Accessed in DeployPlugin rules.
    String component = null
    String targetPlatform = null

    @Override
    void taskDependenciesAvailable(Set<Task> tasks) {
        Set<Task> linkTasks = tasks.findAll { it instanceof AbstractLinkTask }
        if (linkTasks.empty)
            throw new GradleException("${toString()} does not have any link tasks!")
        if (linkTasks.size() > 1)
            throw new GradleException("${toString()} given multiple Link tasks: ${linkTasks}")

        Set<File> files = linkTasks.first().outputs.files.files.findAll { File f -> f.isFile() }
        if (files.empty)
            throw new GradleException("${toString()} Link Task has no output files: ${linkTasks.first()}")

        file.set(files.first())
    }

}
