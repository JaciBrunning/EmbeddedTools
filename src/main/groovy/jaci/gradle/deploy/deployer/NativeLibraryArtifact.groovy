package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic

@CompileStatic
class NativeLibraryArtifact extends FileCollectionArtifact {
    NativeLibraryArtifact(String name) {
        super(name)
        library = name
    }

    String library = null
    List<String> matchers = []
}
