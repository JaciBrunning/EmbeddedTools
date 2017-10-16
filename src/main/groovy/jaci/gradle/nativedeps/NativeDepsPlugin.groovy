package jaci.gradle.nativedeps

import jaci.gradle.EmbeddedTools
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.model.Defaults
import org.gradle.model.Each
import org.gradle.model.Model
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.*
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.platform.base.PlatformContainer

class NativeDepsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

    }

    static class NativeDepsRules extends RuleSource {
        @Model("libraries")
        void createLibrariesModel(NativeDepsSpec spec) { }

        @Defaults
        void setDefaultCombined(@Each CombinedNativeLib combined) {
            combined.setLibs([] as List<String>)
        }

        @Mutate
        void addNativeLibraries(ModelMap<Task> tasks, final Repositories repos, final NativeDepsSpec spec, final ExtensionContainer extensions,
                                final FlavorContainer flavors, final BuildTypeContainer buildTypes, final PlatformContainer platforms) {
            PrebuiltLibraries prelibs = repos.maybeCreate('embeddedtools', PrebuiltLibraries)
            Project project = extensions.getByType(EmbeddedTools.ProjectWrapper).project

            spec.withType(NativeLib).each { NativeLib lib ->
                def libname = lib.backingNode.path.name
                FileTree rootTree, sharedFiles, staticFiles

                def flavor = flavors.getByName(lib.flavor ?: flavors.first().name)
                def buildType = buildTypes.getByName(lib.buildType ?: buildTypes.first().name)
                def platform = platforms.getByName(lib.targetPlatform) as NativePlatform

                if (lib.getMaven() != null) {
                    def cfg = project.configurations.maybeCreate("native_${libname}")
                    project.dependencies.add(cfg.name, lib.getMaven())

                    rootTree = project.zipTree(cfg.dependencies.collectMany { cfg.files(it) }.first())
                } else if (lib.getFile().isDirectory()) {
                    rootTree = project.fileTree(lib.getFile())
                } else {
                    // Assume ZIP File
                    rootTree = project.zipTree(lib.getFile())
                }

                sharedFiles = rootTree.matching { pat -> pat.include(lib.sharedMatchers) }
                staticFiles = rootTree.matching { pat -> pat.include(lib.staticMatchers) }

                Set<File> headerDirs = lib.headerDirs.collect { new File(rootTree.asFileTrees.first().dir, it) }
                FileCollection headerFiles = project.files(headerDirs)
                prelibs.create(libname) { PrebuiltLibrary pl ->
                    NativeLibBinary natLib = new NativeLibBinary(pl.name, headerFiles, staticFiles + sharedFiles, sharedFiles, platform, flavor, buildType)
                    pl.binaries.add(natLib)
                    pl.headers.srcDirs.addAll(headerDirs)
                }
            }

            spec.withType(CombinedNativeLib).each { CombinedNativeLib lib ->
                def libs = lib.libs.collect { prelibs.getByName(it) }

                def binaries = libs.collect { it.binaries.first() }
                def linkFiles = binaries.collect { it.linkFiles }.inject { a, b -> a+b }
                def runtimeFiles = binaries.collect { it.runtimeFiles }.inject { a, b -> a+b }
                def headerFiles = binaries.collect { it.headerDirs }.inject { a, b -> a+b }

                def flavor = flavors.getByName(lib.flavor ?: flavors.first().name)
                def buildType = buildTypes.getByName(lib.buildType ?: buildTypes.first().name)
                def platform = platforms.getByName(lib.targetPlatform) as NativePlatform

                prelibs.create(lib.backingNode.path.name) { PrebuiltLibrary pl ->
                    NativeLibBinary natLib = new NativeLibBinary(pl.name, headerFiles, linkFiles, runtimeFiles, platform, flavor, buildType)
                    pl.binaries.add(natLib)
                    libs.each { pl.headers.srcDirs.addAll(it.headers.srcDirs) }
                }
            }
        }
    }
}
