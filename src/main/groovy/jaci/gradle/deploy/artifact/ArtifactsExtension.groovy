package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.Action
import jaci.gradle.Resolver
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.DefaultNamedDomainObjectSet
import org.gradle.internal.reflect.DirectInstantiator

// DefaultNamedDomainObjectSet applies the withType, matching, all and other methods
// that are incredibly useful
@CompileStatic
class ArtifactsExtension extends DefaultNamedDomainObjectSet<Artifact> implements Resolver<Artifact> {
    final Project project

    ArtifactsExtension(Project project) {
        super(Artifact.class, DirectInstantiator.INSTANCE)
        this.project = project
    }

    public <T extends Artifact> Artifact  artifact(String name, Class<T> type, final Action<T> config) {
        Artifact artifact = project.objects.newInstance(type, name, project)
        config.execute(artifact);
        this << (artifact)
        return artifact
    }

    Artifact fileArtifact(String name, final Action<? extends FileArtifact> config) {
        return artifact(name, FileArtifact, config)
    }

    Artifact fileCollectionArtifact(String name, final Action<? extends FileCollectionArtifact> config) {
        return artifact(name, FileCollectionArtifact, config)
    }

    Artifact fileTreeArtifact(String name, final Action<? extends FileTreeArtifact> config) {
        return artifact(name, FileTreeArtifact, config)
    }

    Artifact commandArtifact(String name, final Action<? extends CommandArtifact> config) {
        return artifact(name, CommandArtifact, config)
    }

    Artifact javaArtifact(String name, final Action<? extends JavaArtifact> config) {
        return artifact(name, JavaArtifact, config)
    }

    Artifact nativeArtifact(String name, final Action<? extends NativeArtifact> config) {
        return artifact(name, NativeArtifact, config)
    }

    Artifact nativeLibraryArtifact(String name, final Action<? extends NativeLibraryArtifact> config) {
        return artifact(name, NativeLibraryArtifact, config)
    }

    Artifact binaryLibraryArtifact(String name, final Action<? extends BinaryLibraryArtifact> config) {
        return artifact(name, BinaryLibraryArtifact, config)
    }

    Artifact mavenArtifact(String name, final Action<? extends MavenArtifact> config) {
        return artifact(name, MavenArtifact, config)
    }

    @Override
    Artifact resolve(Object o) {
        Artifact result = null
        if (o instanceof String)
            result = this.findByName(o.toString())
        else if (o instanceof Artifact)
            result = (Artifact)o
        // TODO more resolution methods

        if (result == null)
            throw new ResolveFailedException("Could not find artifact " + o.toString() + " (" + o.class.name + ")")

        return result
    }
}
