package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeDependencySet

@CompileStatic
class DelegatedDependencySet implements NativeDependencySet {

    Project project
    String name
    NativeBinarySpec binary

    DelegatedDependencySet(Project project, String name, NativeBinarySpec bin) {
        this.project = project
        this.name = name
        this.binary = bin
    }

    // TODO: Error message on missing, FileCollection functions below will throw NPE currently
    ETNativeDepSet get() {
        return project.extensions.getByType(DependencySpecExtension).find(name, binary)
    }

    @Override
    FileCollection getIncludeRoots() {
        return get().getIncludeRoots()
    }

    @Override
    FileCollection getLinkFiles() {
        return get().getLinkFiles()
    }

    @Override
    FileCollection getRuntimeFiles() {
        return get().getRuntimeFiles()
    }

    FileCollection getSourceFiles() {
        return get().getSources()
    }
}
