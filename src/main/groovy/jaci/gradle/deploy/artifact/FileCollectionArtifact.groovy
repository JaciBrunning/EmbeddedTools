package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.Resolver
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.sessions.context.DeployContext
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property

@CompileStatic
class FileCollectionArtifact extends AbstractArtifact implements CacheableArtifact {

    FileCollectionArtifact(Project project, String name) {
        super(project, name)

        files = project.objects.property(FileCollection.class)
    }

    Property<FileCollection> files

    Object cache = null
    Resolver<CacheMethod> cacheResolver

    @Override
    void deploy(DeployContext context) {
        if (files.isPresent())
            context.put(files.get().files, cacheResolver.resolve(cache))
        else
            context.logger.log("No file(s) provided for ${toString()}")
    }

}
