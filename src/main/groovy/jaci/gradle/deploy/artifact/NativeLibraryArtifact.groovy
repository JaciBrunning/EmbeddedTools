package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.nativedeps.DependencySpecExtension
import jaci.gradle.nativedeps.ETNativeDepSet
import org.gradle.api.file.FileCollection

@CompileStatic
class NativeLibraryArtifact extends FileCollectionArtifact {
    NativeLibraryArtifact(String name) {
        super(name)
        library = name
    }

    String library = null
    String targetPlatform = null
    String flavor = null
    String buildType = null

    @Override
    void deploy(DeployContext ctx) {
        def candidates = ctx.project.extensions.getByType(DependencySpecExtension).sets.findAll { ETNativeDepSet set ->
            set.name.equals(library) && set.appliesTo(flavor, buildType, targetPlatform)
        } as List<ETNativeDepSet>

        files.set(candidates.collect { it.getRuntimeFiles() }.inject { a, b -> a+b } as FileCollection)
        super.deploy(ctx)
    }
}
