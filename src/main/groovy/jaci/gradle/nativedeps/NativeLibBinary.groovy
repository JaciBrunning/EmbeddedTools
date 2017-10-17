package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.api.file.FileCollection
import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.SharedLibraryBinary
import org.gradle.nativeplatform.StaticLibraryBinary
import org.gradle.nativeplatform.platform.NativePlatform

@CompileStatic
class NativeLibBinary implements SharedLibraryBinary, StaticLibraryBinary {

    String name
    FileCollection headerDirs, linkerFiles, matchedLibraries, runtimeLibraries
    List<String> libNames
    NativePlatform targetPlatform
    Flavor flavor
    BuildType buildType

    NativeLibBinary(String name, FileCollection headerDirs, FileCollection linkerFiles,
                    FileCollection matchedLibraries, List<String> libNames,
                    FileCollection runtimeLibraries,
                    NativePlatform targetPlatform, Flavor flavor, BuildType buildType) {
        this.name = name
        this.headerDirs = headerDirs
        this.targetPlatform = targetPlatform
        this.flavor = flavor
        this.buildType = buildType
        this.matchedLibraries = matchedLibraries
        this.linkerFiles = linkerFiles
        this.libNames = libNames
        this.runtimeLibraries = runtimeLibraries
    }

    @Override
    FileCollection getLinkFiles() {
        return matchedLibraries
    }

    @Override
    FileCollection getRuntimeFiles() {
        return matchedLibraries
    }

    @Override
    Flavor getFlavor() {
        return flavor
    }

    @Override
    NativePlatform getTargetPlatform() {
        return targetPlatform
    }

    @Override
    BuildType getBuildType() {
        return buildType
    }

    @Override
    String getDisplayName() {
        return name
    }

    @Override
    File getSharedLibraryFile() {
        return null
    }

    @Override
    File getSharedLibraryLinkFile() {
        return null
    }

    @Override
    File getStaticLibraryFile() {
        return null
    }
}
