package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic

@CompileStatic
class NativeLibraryArtifact extends FileCollectionArtifact {
    NativeLibraryArtifact(String name) {
        super(name)
        library = name
    }

    String library = null
    String targetPlatform = null
    List<String> matchers = []
}
