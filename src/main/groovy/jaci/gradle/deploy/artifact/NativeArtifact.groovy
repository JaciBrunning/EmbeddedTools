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

    // Set after deploy logic
    File _nativefile

    @Override
    void deploy(Project project, DeployContext ctx) {
        _nativefile = taskDependencies.findAll { it instanceof AbstractLinkTask }.collect { Task t ->
            t.outputs.files.files.findAll { File f -> f.isFile() }.first()
        }.first()
        ctx.put(_nativefile, (filename == null ? _nativefile.name : filename), cache)
    }
}
