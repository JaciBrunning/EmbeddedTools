package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.cache.Cacheable
import org.gradle.api.Project
import org.gradle.api.tasks.TaskOutputs

@CompileStatic
class NativeArtifact extends ArtifactBase implements Cacheable {
    NativeArtifact(String name) {
        super(name)
        component = name
    }

    String component = null
    String targetPlatform = null

    String filename = null

    // Calculated Values
    TaskOutputs linkOut = null

    @Override
    void deploy(Project project, DeployContext ctx) {
        File file = linkOut.files.files.first()
        ctx.put(file, (filename == null ? file.name : filename), cache)
    }
}
