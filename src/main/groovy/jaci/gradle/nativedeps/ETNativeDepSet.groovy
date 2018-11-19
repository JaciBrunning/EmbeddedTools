package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import jaci.gradle.files.IDirectoryTree
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.NativeDependencySet
import org.gradle.nativeplatform.platform.NativePlatform

import java.util.concurrent.Callable

@CompileStatic
public class ETNativeDepSet implements NativeDependencySet, SystemLibsDependencySet {

    Project         project
    String          name
    FileCollection  staticLibs, sharedLibs, dynamicLibs, debugLibs
    IDirectoryTree  headers, sources
    List<String>    systemLibs
    NativePlatform  targetPlatform
    Flavor          flavor
    BuildType       buildType

    public ETNativeDepSet(Project project, String name,
                          IDirectoryTree headers, IDirectoryTree sources,
                          FileCollection staticLibs, FileCollection sharedLibs,
                          FileCollection dynamicLibs, FileCollection debugLibs,
                          List<String> systemLibs, NativePlatform targetPlatform,
                          Flavor flavor, BuildType buildType) {
        this.project = project
        this.name = name

        this.headers = headers
        this.sources = sources

        this.staticLibs = staticLibs
        this.sharedLibs = sharedLibs
        this.dynamicLibs = dynamicLibs
        this.debugLibs = debugLibs
        this.systemLibs = systemLibs

        this.targetPlatform = targetPlatform
        this.flavor = flavor
        this.buildType = buildType
    }

    @Override
    FileCollection getIncludeRoots() {
        return project.files( { headers.getDirectories() } as Callable<Set<File>> )
    }

    FileCollection getSourceRoots() {
        return project.files( { sources.getDirectories() } as Callable<Set<File>> )
    }

    @Override
    FileCollection getLinkFiles() {
        return sharedLibs + staticLibs
    }

    @Override
    FileCollection getRuntimeFiles() {
        // Needed to have a flat set, as otherwise the install tasks do not work
        // properly
        return project.files {
            return dynamicLibs.files
        }
    }

    FileCollection getDebugFiles() {
        return debugLibs
    }

    boolean appliesTo(Flavor flav, BuildType btype, NativePlatform plat) {
        if (flavor != null && flavor != flav)
            return false
        if (buildType != null && buildType != btype)
            return false
        if (targetPlatform != plat)
            return false

        return true
    }

    boolean appliesTo(String flavorName, String buildTypeName, String platformName) {
        if (flavor != null && !flavor.name.equals(flavorName))
            return false
        if (buildType != null && !buildType.name.equals(buildTypeName))
            return false
        if (!targetPlatform.name.equals(platformName))
            return false

        return true
    }

    @Override
    String toString() {
        return "ETNativeDepSet[${name} F:${flavor} BT:${buildType} P:${targetPlatform}]"
    }
}
