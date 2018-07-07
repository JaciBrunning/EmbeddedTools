package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Task
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.tasks.bundling.Jar

@CompileStatic
class JavaArtifact extends FileArtifact implements TaskHungryArtifact {
    JavaArtifact(String name) {
        super(name)
    }

    String jar

    void setJar(Object jarNotation) {
        this.jar = jarNotation
        dependsOn(jarNotation)
    }

    @Override
    void taskDependenciesAvailable(Set<Task> tasks) {
        file.set(tasks.findAll { it instanceof Jar }.collect { Task t -> t.outputs.files.files.first() }.first())
    }
}
