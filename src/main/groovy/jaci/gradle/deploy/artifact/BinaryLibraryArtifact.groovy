package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.nativeplatform.NativeBinarySpec

@CompileStatic
class BinaryLibraryArtifact extends FileCollectionArtifact {

    BinaryLibraryArtifact(String name, Project project) {
        super(name, project)
    }

    NativeBinarySpec binary

    @Override
    void deploy(DeployContext ctx) {
        def libs = binary.libs.collect { it.getRuntimeFiles() }
        if (libs.size() != 0 ) {
            files.set(libs.inject { a,b -> a + b } as FileCollection)
            super.deploy(ctx)
        }
    }
}
