package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.cache.Cacheable
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@CompileStatic
class FileCollectionArtifact extends ArtifactBase implements Cacheable {
    FileCollectionArtifact(String name) {
        super(name)
    }

    FileCollection files = null

    @Override
    void deploy(Project project, DeployContext ctx) {
        ctx.put(files.files, cache)
    }
}
