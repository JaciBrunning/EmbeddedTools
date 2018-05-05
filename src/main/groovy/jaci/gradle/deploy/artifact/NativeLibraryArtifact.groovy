package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.nativedeps.DependencySpecExtension
import jaci.gradle.nativedeps.ETNativeDepSet
import org.gradle.api.Project

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
    void deploy(Project project, DeployContext ctx) {
        def candidates = project.extensions.getByType(DependencySpecExtension).sets.findAll { ETNativeDepSet set ->
            boolean valid = set.name.equals(library)
            if (targetPlatform != null && !set.targetPlatform.name.equals(targetPlatform))
                valid = false
            if (flavor != null && set.flavor != null && !set.flavor.name.equals(flavor))
                valid = false
            if (buildType != null && set.buildType != null && !set.buildType.name.equals(buildType))
                valid = false
            valid
        } as List<ETNativeDepSet>

        files = candidates.collect { it.getRuntimeFiles() }.inject { a, b -> a+b }
        super.deploy(project, ctx)
    }
}
