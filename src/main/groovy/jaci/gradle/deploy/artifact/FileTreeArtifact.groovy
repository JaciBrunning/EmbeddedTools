package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.Resolver
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.provider.Property

@CompileStatic
class FileTreeArtifact extends AbstractArtifact implements CacheableArtifact {

    FileTreeArtifact(String name, Project project) {
        super(name, project)
        files = project.objects.property(FileTree.class)
    }

    final Property<FileTree> files

    void setFiles(FileTree tree) {
        this.files.set(tree)
    }

    Object cache = "md5sum"
    Resolver<CacheMethod> cacheResolver

    @Override
    void deploy(DeployContext context) {
        if (files.isPresent()) {
            Map<String, File> f = [:]
            Set<String> mkdirs = []
            // TODO: we can probably use filevisit in dep root finding.
            files.get().visit { FileVisitDetails details ->
                if (details.isDirectory())
                    mkdirs << details.path
                else
                    f[details.path] = details.file
            }
            context.execute("mkdir -p ${mkdirs.join(' ')}")
            context.put(f, cacheResolver?.resolve(cache))
        } else
            context.logger?.log("No file tree provided for ${toString()}")
    }

}
