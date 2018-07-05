package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.Resolver
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.provider.DefaultPropertyState
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

@CompileStatic
class FileCollectionArtifact extends AbstractArtifact implements CacheableArtifact {
    FileCollectionArtifact(String name) {
        super(name)
    }

    Property<FileCollection> files = new DefaultPropertyState<>(FileCollection)

    Object cache = null

    Resolver<CacheMethod> cacheResolver

    @Override
    void deploy(DeployContext context) {
        if (files.isPresent())
            context.put(files.get().files, cacheResolver.resolve(cache))
    }
}
