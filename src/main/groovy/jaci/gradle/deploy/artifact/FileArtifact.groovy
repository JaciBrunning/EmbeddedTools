package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.Resolver
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project
import org.gradle.api.provider.Property

@CompileStatic
class FileArtifact extends AbstractArtifact implements CacheableArtifact {

    FileArtifact(Project project, String name) {
        super(project, name)

        file = project.objects.property(File.class)
    }

    Property<File> file
    String filename = null

    Object cache = null

    Resolver<CacheMethod> cacheResolver

    @Override
    void deploy(DeployContext context) {
        if (file.isPresent()) {
            File f = file.get()
            context.put(f, (filename == null ? f.name : filename), cacheResolver.resolve(cache))
        } else
            context.logger().log("No file provided for ${toString()}")
    }

}
