package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.cache.Cacheable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.nativeplatform.tasks.AbstractLinkTask

@CompileStatic
class NativeArtifact extends ArtifactBase implements Cacheable {
    NativeArtifact(String name) {
        super(name)
        component = name
    }

    String component = null
    String targetPlatform = null

    String filename = null

    @Override
    void deploy(Project project, DeployContext ctx) {
        File file = taskDependencies.findAll { it instanceof AbstractLinkTask }.collect { Task t -> t.outputs.files.files.first() }.first()
        ctx.put(file, (filename == null ? file.name : filename), cache)
    }
}
