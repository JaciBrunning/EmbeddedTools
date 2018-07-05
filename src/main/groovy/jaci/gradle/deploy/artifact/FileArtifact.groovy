package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.Resolver
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.internal.provider.DefaultPropertyState
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

@CompileStatic
class FileArtifact extends AbstractArtifact implements CacheableArtifact {
    FileArtifact(String name) {
        super(name)
    }

    Property<File> file = new DefaultPropertyState<>(File.class)
    String filename = null

    Object cache = null

    Resolver<CacheMethod> cacheResolver

    @Override
    void deploy(DeployContext context) {
        File f = file.get()
        context.put(f, (filename == null ? f.name : filename), cacheResolver.resolve(cache))
    }
}
