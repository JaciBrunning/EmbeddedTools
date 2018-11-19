package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.api.file.FileCollection
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeDependencySet

@CompileStatic
class DelegatedDependencySet implements NativeDependencySet, SystemLibsDependencySet {

    String name
    NativeBinarySpec binary
    DependencySpecExtension ext
    boolean skipOnNonFoundDependency

    DelegatedDependencySet(String name, NativeBinarySpec bin, DependencySpecExtension ext) {
        this.name = name
        this.binary = bin
        this.ext = ext
    }

    DelegatedDependencySet(String name, NativeBinarySpec bin, DependencySpecExtension ext, boolean skipUd) {
        this(name, bin, ext)
        this.skipOnNonFoundDependency = skipUd
    }

    ETNativeDepSet get() {
        def ds = ext.find(name, binary)
        if (ds == null && !skipOnNonFoundDependency)
            throw new MissingDependencyException(name, binary)
        return ds
    }

    @Override
    FileCollection getIncludeRoots() {
        def depSet =  get()
        if (depSet == null) {
            return ext.project.files()
        }
        return depSet.getIncludeRoots()
    }

    @Override
    FileCollection getLinkFiles() {
        def depSet =  get()
        if (depSet == null) {
            return ext.project.files()
        }
        return depSet.getLinkFiles()
    }

    @Override
    FileCollection getRuntimeFiles() {
        def depSet =  get()
        if (depSet == null) {
            return ext.project.files()
        }
        return depSet.getRuntimeFiles()
    }

    FileCollection getSourceFiles() {
        def depSet =  get()
        if (depSet == null) {
            return ext.project.files()
        }
        return depSet.getSourceRoots()
    }

    FileCollection getDebugFiles() {
        def depSet =  get()
        if (depSet == null) {
            return ext.project.files()
        }
        return depSet.getDebugFiles()
    }

    @Override
    List<String> getSystemLibs() {
        def depSet =  get()
        if (depSet == null) {
            return []
        }
        return depSet.getSystemLibs()
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
