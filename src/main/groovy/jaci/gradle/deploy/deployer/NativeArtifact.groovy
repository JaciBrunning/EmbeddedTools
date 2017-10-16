package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.cache.Cacheable
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
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

    boolean libraries = false
    String  libraryDir = null
    def     libcache = "md5file"

    // Calculated Values
    TaskOutputs linkOut = null
    FileCollection libraryFiles = null

    @Override
    void deploy(Project project, DeployContext ctx) {
        File file = linkOut.files.files.first()
        ctx.put(file, (filename == null ? file.name : filename), cache)

        if (libraries && libraryFiles != null) {
            def context = ctx.subContext(libraryDir)
            libraryFiles.files.each { f ->
                context.put(f, f.name, libcache)
            }
        }
    }
}
