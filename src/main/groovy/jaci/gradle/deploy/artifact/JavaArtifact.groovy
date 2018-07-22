package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar

import java.util.concurrent.Callable

@CompileStatic
class JavaArtifact extends FileArtifact implements TaskHungryArtifact {

    JavaArtifact(String name, Project project) {
        super(name, project)
        dependsOn({ jar } as Callable<Object>)
    }

    Object jar = "jar"

    @Override
    void taskDependenciesAvailable(Set<Task> tasks) {
        Set<Task> jarTasks = tasks.findAll { it instanceof Jar }   // JarTask existence is already checked in dependsOn
        if (jarTasks.size() > 1)
            throw new GradleException("${toString()} given multiple Jar tasks: ${jarTasks}")

        Set<File> files = jarTasks.first().outputs.files.files
        if (files.empty)
            throw new GradleException("${toString()} Jar Task has no output files: ${jarTasks.first()}")

        file.set(files.first())
    }
}
