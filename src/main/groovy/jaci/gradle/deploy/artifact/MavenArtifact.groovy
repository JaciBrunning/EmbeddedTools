package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.Resolver
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

import javax.inject.Inject

@CompileStatic
class MavenArtifact extends FileArtifact {
    @Inject
    MavenArtifact(String name, Project project) {
        super(name, project)
    }

    Dependency dependency
    Configuration configuration

    @Override
    void deploy(DeployContext ctx) {
        if (configuration == null || dependency == null) {
            ctx.logger?.log("No configuration or dependency set")
            return
        }
        def files = configuration.files(dependency)
        if (files.size() == 1) {
            File f = files.first()
            file.set(f)
            super.deploy(ctx)
        } else {
            ctx.logger?.log("Incorrect number of files found for ${dependency.toString()}")
        }
    }
}
