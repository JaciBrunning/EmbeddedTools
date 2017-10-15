package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.cache.Cacheable
import org.gradle.api.Project

@CompileStatic
class FileArtifact extends ArtifactBase implements Cacheable {
    FileArtifact(String name) {
        super(name)
    }

    File file       = null
    String filename = null

    @Override
    void deploy(Project project, DeployContext ctx) {
        ctx.put(file, (filename == null ? file.name : filename), cache)
    }
}
