package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic

@CompileStatic
class NativeLibraryArtifact extends FileCollectionArtifact {
    NativeLibraryArtifact(String name) {
        super(name)
        cache = 'md5file'
    }

    String library = null
}
