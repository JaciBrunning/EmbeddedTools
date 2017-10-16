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
    FileCollection headerDirs, linkFiles, runtimeFiles
    NativePlatform targetPlatform
    Flavor flavor
    BuildType buildType
    List<FileCollection> linkerLibs

    NativeLibBinary(String name, FileCollection headerDirs, FileCollection linkFiles, FileCollection runtimeFiles,
                    NativePlatform targetPlatform, Flavor flavor, BuildType buildType, List<FileCollection> linkerLibs) {
        this.name = name
        this.headerDirs = headerDirs
        this.linkFiles = linkFiles
        this.runtimeFiles = runtimeFiles
        this.targetPlatform = targetPlatform
        this.flavor = flavor
        this.buildType = buildType
        this.linkerLibs = linkerLibs
    }

    @Override
    FileCollection getHeaderDirs() {
        return headerDirs
    }

    @Override
    FileCollection getLinkFiles() {
        return linkFiles
    }

    @Override
    FileCollection getRuntimeFiles() {
        return runtimeFiles
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

    // We've overridden above
    @Override
    File getSharedLibraryFile() {
        return runtimeFiles.first()
    }

    @Override
    File getSharedLibraryLinkFile() {
        return runtimeFiles.first()
    }

    @Override
    File getStaticLibraryFile() {
        return linkFiles.first()
    }
}
