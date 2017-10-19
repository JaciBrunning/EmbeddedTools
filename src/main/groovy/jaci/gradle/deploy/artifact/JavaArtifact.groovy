package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar

@CompileStatic
class JavaArtifact extends FileCollectionArtifact {
    JavaArtifact(String name) {
        super(name)
    }

    String jar
    String filename = null

    // Set after deploy logic
    File _jarfile

    void setJar(Object jarNotation) {
        this.jar = jarNotation
        dependsOn(jarNotation)
    }

    @Override
    void deploy(Project project, DeployContext ctx) {
        _jarfile = taskDependencies.findAll { it instanceof Jar }.collect { Task t -> t.outputs.files.files.first() }.first()
        ctx.put(_jarfile, (filename == null ? _jarfile.name : filename), cache)
    }
}
