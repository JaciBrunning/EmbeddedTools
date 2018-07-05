package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.DefaultNamedDomainObjectSet
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

// DefaultNamedDomainObjectSet applies the withType, matching, all and other methods
// that are incredibly useful
@CompileStatic
class ArtifactsExtension extends DefaultNamedDomainObjectSet<Artifact> {
    Project project

    @Inject
    ArtifactsExtension(Project project) {
        super(Artifact.class, DirectInstantiator.INSTANCE)
        this.project = project
    }

    def artifact(String name, Class<? extends Artifact> type, final Closure config) {
        def artifact = type.newInstance(name)
        project.configure(artifact, config)
        this << (artifact)
    }
//
//    def fileArtifact(String name, final Closure config) {
//        artifact(name, FileArtifact, config)
//    }
//
//    def fileCollectionArtifact(String name, final Closure config) {
//        artifact(name, FileCollectionArtifact, config)
//    }
//
//    def commandArtifact(String name, final Closure config) {
//        artifact(name, CommandArtifact, config)
//    }
//
//    def javaArtifact(String name, final Closure config) {
//        artifact(name, JavaArtifact, config)
//    }
//
////    def nativeArtifact(String name, final Closure config) {
////        artifact(name, NativeArtifact, config)
////    }
//
//    def nativeLibraryArtifact(String name, final Closure config) {
//        artifact(name, NativeLibraryArtifact, config)
//    }
}
