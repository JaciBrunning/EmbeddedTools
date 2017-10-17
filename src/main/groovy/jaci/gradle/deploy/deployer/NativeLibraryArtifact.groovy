package jaci.gradle.deploy.deployer

class NativeLibraryArtifact extends FileCollectionArtifact {
    NativeLibraryArtifact(String name) {
        super(name)
        cache = 'md5file'
    }

    String library = null
}
