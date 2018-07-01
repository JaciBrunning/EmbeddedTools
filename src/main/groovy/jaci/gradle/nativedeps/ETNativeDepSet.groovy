package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import jaci.gradle.files.AbstractDirectoryTree
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.collections.FileCollectionAdapter
import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.NativeDependencySet
import org.gradle.nativeplatform.platform.NativePlatform

import java.util.concurrent.Callable

@CompileStatic
public class ETNativeDepSet implements NativeDependencySet {

    Project         project
    String          name
    FileCollection  staticLibs, sharedLibs, dynamicLibs
    AbstractDirectoryTree headers, sources
    List<String>    systemLibs
    NativePlatform  targetPlatform
    Flavor          flavor
    BuildType       buildType

    public ETNativeDepSet(Project project, String name,
                          AbstractDirectoryTree headers, AbstractDirectoryTree sources,
                          FileCollection staticLibs, FileCollection sharedLibs,
                          FileCollection dynamicLibs, List<String> systemLibs,
                          NativePlatform targetPlatform, Flavor flavor, BuildType buildType) {
        this.project = project
        this.name = name

        this.headers = headers
        this.sources = sources

        this.staticLibs = staticLibs
        this.sharedLibs = sharedLibs
        this.dynamicLibs = dynamicLibs
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
        return project.files {
            return dynamicLibs.files
        }
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
}
