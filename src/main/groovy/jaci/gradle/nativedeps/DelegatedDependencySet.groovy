package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.api.file.FileCollection
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeDependencySet

@CompileStatic
class DelegatedDependencySet implements NativeDependencySet {

    String name
    NativeBinarySpec binary
    DependencySpecExtension ext

    DelegatedDependencySet(String name, NativeBinarySpec bin, DependencySpecExtension ext) {
        this.name = name
        this.binary = bin
        this.ext = ext
    }

    ETNativeDepSet get() {
        def ds = ext.find(name, binary)
        if (ds == null)
            throw new MissingDependencyException(name, binary)
        return ds
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
        return get().getSourceRoots()
    }

    @CompileStatic
    static class MissingDependencyException extends RuntimeException {
        String dependencyName
        NativeBinarySpec binary

        MissingDependencyException(String name, NativeBinarySpec binary) {
            super("Cannot find delegated dependency: ${name} for binary: ${binary}".toString())
            this.dependencyName = name
            this.binary = binary
        }
    }
}
