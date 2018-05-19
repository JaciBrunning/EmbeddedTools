package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.SortUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.base.LanguageSourceSet
import org.gradle.model.*
import org.gradle.nativeplatform.*
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.platform.base.BinaryTasks
import org.gradle.platform.base.PlatformContainer

@CompileStatic
class NativeDepsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        DependencySpecExtension dse = project.extensions.create("ETDependencySpecs", DependencySpecExtension, project)

        project.extensions.add("useLibrary", { Object closureArg, String... names ->
            if (closureArg in TargetedNativeComponent) {
                TargetedNativeComponent component = (TargetedNativeComponent)closureArg
                component.binaries.withType(NativeBinarySpec).all { NativeBinarySpec bin ->
                    names.each { String name ->
                        DelegatedDependencySet set = new DelegatedDependencySet(project, name, bin)
                        bin.lib(set)
                    }
                }
            } else if (closureArg in NativeBinarySpec) {
                NativeBinarySpec bin = (NativeBinarySpec)closureArg
                names.each { String name ->
                    DelegatedDependencySet set = new DelegatedDependencySet(project, name, bin)
                    bin.lib(set)
                }
            } else if (closureArg in LanguageSourceSet) {
                throw new GradleException('The useLibrary command needs to be placed directly in the component. Move it outside of the sources declaration.')
            } else {
                throw new GradleException('Unknown type, You put this declaration in a weird place...')
            }
        })
    }

    static class NativeDepsRules extends RuleSource {
        @Model("libraries")
        void createLibrariesModel(NativeDepsSpec spec) { }

        @Defaults
        void setDefaultCombined(@Each CombinedNativeLib combined) {
            combined.setLibs([] as List<String>)
        }

        @Mutate
        void addMavenDeps(final NativeDepsSpec spec, final ExtensionContainer extensions) {
            Project project = extensions.getByType(EmbeddedTools.ProjectWrapper).project

            // Add Maven Dependencies if necessary
            spec.withType(NativeLib) {
                if (it.getMaven() != null) {
                    def cfg = project.configurations.maybeCreate("native_${it.name}")
                    project.dependencies.add(cfg.name, it.getMaven())
                }
            }
        }

        @Mutate
        void addNativeLibraries(ModelMap<Task> tasks, final Repositories repos,
                                final NativeDepsSpec spec, final ExtensionContainer extensions,
                                final FlavorContainer flavors, final BuildTypeContainer buildTypes, final PlatformContainer platforms) {

            // Add the library bindings, so we can access them from the native component configuration.
            // This must be different from addMavenDeps since by now, the dependencies of the project are 'locked'
            // and set as read-only.
            Project project = extensions.getByType(EmbeddedTools.ProjectWrapper).project
            DependencySpecExtension dse = extensions.getByType(DependencySpecExtension)

            spec.withType(NativeLib).each { lib ->
                def binname = lib.name
                def libname = lib.libraryName ?: lib.name
                FileTree rootTree, sharedFiles, staticFiles, dynamicFiles

                def flavor = lib.flavor == null ? null as Flavor : flavors.findByName(lib.flavor)
                def buildType = lib.buildType == null ? null as BuildType : buildTypes.findByName(lib.buildType)
                def tPlatforms = [] as List<NativePlatform>
                if (lib.targetPlatforms != null && lib.targetPlatforms.size() > 0) {
                    lib.targetPlatforms.each { String p ->
                        tPlatforms.add(platforms.getByName(p) as NativePlatform)
                    }
                } else {
                    tPlatforms = [platforms.getByName(lib.targetPlatform) as NativePlatform]
                }

                if (lib.getMaven() != null) {
                    def cfg = project.configurations.getByName("native_${binname}")

                    rootTree = project.zipTree(cfg.dependencies.collectMany { cfg.files(it) as Collection }.first())
                } else if (lib.getFile().isDirectory()) {
                    rootTree = project.fileTree(lib.getFile())
                } else {
                    // Assume ZIP File
                    rootTree = project.zipTree(lib.getFile())
                }

                sharedFiles = rootTree.matching { PatternFilterable pat -> pat.include(lib.sharedMatchers  ?: ['<<EMBEDDEDTOOLS_NOMATCH>>']) }
                staticFiles = rootTree.matching { PatternFilterable pat -> pat.include(lib.staticMatchers  ?: ['<<EMBEDDEDTOOLS_NOMATCH>>']) }
                dynamicFiles = rootTree.matching { PatternFilterable pat -> pat.include(lib.dynamicMatchers ?: ['<<EMBEDDEDTOOLS_NOMATCH>>']) }

                def headerFiles = new PreemptiveDirectoryFileCollection(rootTree, lib.headerDirs ?: [] as List<String>)
                def sourceFiles = new PreemptiveDirectoryFileCollection(rootTree, lib.sourceDirs ?: [] as List<String>)

                tPlatforms.each { NativePlatform platform ->
                    ETNativeDepSet depSet = new ETNativeDepSet(
                        project,
                        libname,
                        headerFiles,
                        sourceFiles,
                        staticFiles,
                        sharedFiles,
                        dynamicFiles,
                        lib.systemLibs ?: [] as List<String>,
                        platform,
                        flavor,
                        buildType
                    )
                    dse.sets.add(depSet)
                }
                null
            }

            List<SortUtils.TopoMember<CombinedNativeLib>> unsorted = []
            spec.withType(CombinedNativeLib).each { lib ->
                def member = new SortUtils.TopoMember<CombinedNativeLib>()
                member.name = lib.name
                member.dependsOn = lib.libs
                member.extra = lib
                unsorted << member
            }

            List<SortUtils.TopoMember<CombinedNativeLib>> sorted = SortUtils.topoSort(unsorted)
            sorted.each { SortUtils.TopoMember<CombinedNativeLib> member ->
                CombinedNativeLib lib = member.extra
                def libname = lib.libraryName ?: lib.name

                def flavor = lib.flavor == null ? null as Flavor : flavors.findByName(lib.flavor)
                def buildType = lib.buildType == null ? null as BuildType : buildTypes.findByName(lib.buildType)
                def tPlatforms = [] as List<NativePlatform>
                if (lib.targetPlatforms != null && lib.targetPlatforms.size() > 0) {
                    lib.targetPlatforms.each { String p ->
                        tPlatforms.add(platforms.getByName(p) as NativePlatform)
                    }
                } else {
                    tPlatforms = [platforms.getByName(lib.targetPlatform) as NativePlatform]
                }

                tPlatforms.each { NativePlatform platform ->
                    def libs = lib.libs.collect { dse.find(it, flavor, buildType, platform) }
                    def headers = libs.collect { it.headers }.inject { a, b -> a+b }
                    def sources = libs.collect { it.sources }.inject { a, b -> a+b }
                    def staticFiles = libs.collect { it.staticLibs }.inject { a, b -> a+b }
                    def sharedFiles = libs.collect { it.sharedLibs }.inject { a, b -> a+b }
                    def dynamicFiles = libs.collect { it.dynamicLibs }.inject { a, b -> a+b }
                    def systemLibs = libs.collectMany { it.systemLibs as Collection } as List<String>

                    ETNativeDepSet depSet = new ETNativeDepSet(
                        project,
                        libname,
                        headers,
                        sources,
                        staticFiles,
                        sharedFiles,
                        dynamicFiles,
                        systemLibs,
                        platform,
                        flavor,
                        buildType
                    )
                    dse.sets.add(depSet)
                }
            }
        }

        @BinaryTasks
        void addLinkerArgs(ModelMap<Task> tasks, final Repositories repos, final NativeBinarySpec bin) {
            bin.libs.each { NativeDependencySet set ->
                if (set instanceof ETNativeDepSet) {
                    def etnds = set as ETNativeDepSet
                    bin.linker.args.addAll(etnds.getSystemLibs().collectMany { name ->
                        [ "-l", name ]
                    })
                }
            }
        }
    }
}
