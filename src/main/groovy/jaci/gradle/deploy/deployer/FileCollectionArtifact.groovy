package jaci.gradle.deploy.deployer

import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.cache.Cacheable
import org.gradle.api.file.FileCollection

class FileCollectionArtifact extends ArtifactBase implements Cacheable {
    FileCollectionArtifact(String name) {
        super(name)
    }

    FileCollection files = null

    @Override
    void deploy(DeployContext ctx) {
        files.files.each { file ->
            ctx.put(file, file.name, cache)
        }
    }
}
