package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.DefaultNamedDomainObjectSet
import org.gradle.internal.reflect.DirectInstantiator

// DefaultNamedDomainObjectSet applies the withType, matching, all and other methods
// that are incredibly useful
@CompileStatic
class ArtifactsExtension extends DefaultNamedDomainObjectSet<Artifact> {
    Project project

    ArtifactsExtension(Project project) {
        super(Artifact.class, DirectInstantiator.INSTANCE)
        this.project = project
    }

    Artifact artifact(String name, Class<? extends Artifact> type, final Closure config) {
        def artifact = type.newInstance(project, name)
        project.configure(artifact, config)
        this << (artifact)
        return artifact
    }

    Artifact fileArtifact(String name, final Closure config) {
        return artifact(name, FileArtifact, config)
    }

    Artifact fileCollectionArtifact(String name, final Closure config) {
        return artifact(name, FileCollectionArtifact, config)
    }

    Artifact commandArtifact(String name, final Closure config) {
        return artifact(name, CommandArtifact, config)
    }

    Artifact javaArtifact(String name, final Closure config) {
        return artifact(name, JavaArtifact, config)
    }

    Artifact nativeArtifact(String name, final Closure config) {
        return artifact(name, NativeArtifact, config)
    }

    Artifact nativeLibraryArtifact(String name, final Closure config) {
        return artifact(name, NativeLibraryArtifact, config)
    }
}
