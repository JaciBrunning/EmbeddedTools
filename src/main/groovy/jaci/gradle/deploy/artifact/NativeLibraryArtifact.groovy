package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.sessions.context.DeployContext
import jaci.gradle.nativedeps.DependencySpecExtension
import jaci.gradle.nativedeps.ETNativeDepSet
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@CompileStatic
class NativeLibraryArtifact extends FileCollectionArtifact {

    NativeLibraryArtifact(Project project, String name) {
        super(project, name)
        library = name
    }

    String library = null
    String targetPlatform = null
    String flavor = null
    String buildType = null

    @Override
    void deploy(DeployContext ctx) {
        def sets = project.getExtensions().getByType(DependencySpecExtension).sets

        def candidates = sets.findAll { ETNativeDepSet set ->
            set.name.equals(library) && set.appliesTo(flavor, buildType, targetPlatform)
        } as List<ETNativeDepSet>

        if (candidates.empty)
            throw new GradleException("${toString()} cannot find suitable dependency for library ${library}, " +
                    "platform ${targetPlatform}, flavor ${flavor}, buildType ${buildType}")

        files.set(candidates.collect { it.getRuntimeFiles() }.inject { a,b -> a + b } as FileCollection)

        super.deploy(ctx)
    }
}
