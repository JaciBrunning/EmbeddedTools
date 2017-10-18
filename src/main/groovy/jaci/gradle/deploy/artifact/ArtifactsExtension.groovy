package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class ArtifactsExtension extends HashMap<String, ArtifactBase> {
    Project project

    ArtifactsExtension(Project project) {
        this.project = project
    }

    def artifact(String name, Class<? extends ArtifactBase> type, final Closure config) {
        def artifact = type.newInstance(name)
        project.configure(artifact, config)
        this[name] = artifact
    }

    def fileArtifact(String name, final Closure config) {
        artifact(name, FileArtifact, config)
    }

    def fileCollectionArtifact(String name, final Closure config) {
        artifact(name, FileCollectionArtifact, config)
    }

    def commandArtifact(String name, final Closure config) {
        artifact(name, CommandArtifact, config)
    }

    def nativeArtifact(String name, final Closure config) {
        artifact(name, NativeArtifact, config)
    }

    def nativeLibraryArtifact(String name, final Closure config) {
        artifact(name, NativeLibraryArtifact, config)
    }
}
