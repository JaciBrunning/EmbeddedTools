package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.SortUtils
import jaci.gradle.files.FileTreeSupplier
import jaci.gradle.files.DefaultDirectoryTree
import jaci.gradle.files.IDirectoryTree
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.language.base.LanguageSourceSet
import org.gradle.model.*
import org.gradle.nativeplatform.*
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.tasks.AbstractLinkTask
import org.gradle.platform.base.BinaryTasks
import org.gradle.platform.base.PlatformContainer
import org.gradle.platform.base.VariantComponentSpec

import java.util.concurrent.Callable
import java.util.function.Supplier
import java.util.function.Function

@CompileStatic
class NativeDepsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        DependencySpecExtension dse = project.extensions.create("ETDependencySpecs", DependencySpecExtension, project)
    }

    static class NativeDepsRules extends RuleSource {
        @Model("libraries")
        void createLibrariesModel(NativeDepsSpec spec) { }

        @Defaults
        void setDefaultCombined(@Each CombinedNativeLib combined) {
            combined.setLibs([] as List<String>)
        }

        @Mutate
        void addNativeLibs(ModelMap<Task> tasks,
                            final NativeDepsSpec spec, final ExtensionContainer extensions,
                            final FlavorContainer flavors, final BuildTypeContainer buildTypes, final PlatformContainer platforms) {
            Project project = extensions.getByType(EmbeddedTools.ProjectWrapper).project
            DependencySpecExtension dse = extensions.getByType(DependencySpecExtension)

            spec.withType(NativeLib).each { NativeLib lib ->
                def uniqName = lib.name
                def libName = lib.libraryName ?: uniqName

                Supplier<FileTree> rootTree = addDependency(project, lib)

                Flavor flavor = lib.flavor == null ? null : flavors.findByName(lib.flavor)
                BuildType buildType = lib.buildType == null ? null : buildTypes.findByName(lib.buildType)
                List<NativePlatform> targetPlatforms = getPlatforms(lib, platforms)

                FileCollection sharedFiles = matcher(project, rootTree, lib.sharedMatchers, lib.sharedExcludes)
                FileCollection staticFiles = matcher(project, rootTree, lib.staticMatchers, lib.staticExcludes)
                FileCollection debugFiles = matcher(project, rootTree, lib.debugMatchers, lib.debugExcludes)
                FileCollection dynamicFiles = matcher(project, rootTree, lib.dynamicMatchers, lib.dynamicExcludes)

                IDirectoryTree headerFiles = new DefaultDirectoryTree(rootTree, lib.headerDirs ?: [] as List<String>)
                IDirectoryTree sourceFiles = new DefaultDirectoryTree(rootTree, lib.sourceDirs ?: [] as List<String>)

                targetPlatforms.each { NativePlatform platform ->
                    ETNativeDepSet depSet = new ETNativeDepSet(
                        project, libName,
                        headerFiles, sourceFiles, staticFiles + sharedFiles,
                        dynamicFiles, debugFiles, lib.systemLibs ?: [] as List<String>,
                        platform, flavor, buildType
                    )
                    dse.sets.add(depSet)
                }
                null
            }

            sortCombinedLibs(spec.withType(CombinedNativeLib)).each { CombinedNativeLib lib ->
                def uniqName = lib.name
                def libName = lib.libraryName ?: uniqName

                List<Flavor> targetFlavors = getFlavors(lib, flavors)
                List<BuildType> targetBuildTypes = getBuildTypes(lib, buildTypes)
                List<NativePlatform> targetPlatforms = getPlatforms(lib, platforms)

                targetPlatforms.each { NativePlatform platform ->
                    targetFlavors.each { Flavor flavor ->
                        targetBuildTypes.each { BuildType buildType ->
                            ETNativeDepSet dep = mergedDepSet(project, dse, libName, lib.libs, flavor, buildType, platform)
                            dse.sets.add(dep)
                        }
                    }
                }
                null
            }
        }

        @BinaryTasks
        void addLinkerArgs(ModelMap<Task> tasks, final NativeBinarySpec binary) {
            tasks.withType(AbstractLinkTask) { AbstractLinkTask task ->
                task.linkerArgs.addAll(new DefaultProvider<List<String>>({
                    def libs = [] as List<String>
                    binary.libs.each { Object lib ->
                        if (lib instanceof DelegatedDependencySet) {
                            DelegatedDependencySet set = (DelegatedDependencySet)lib
                            libs += set.getSystemLibs()
                        }
                    }
                    libs
                }))
            }
        }

        private static boolean nullSafeEquals(Object a, Object b) {
            return (a == b) || (a != null && b != null && a.equals(b))
        }

        private static File resolve(Set<ResolvedArtifact> artifacts, Dependency dep) {
            def selected = artifacts.findAll { ResolvedArtifact art ->
                def mid = art.moduleVersion.id
                boolean applies = false

                if (nullSafeEquals(mid.group, dep.group) && nullSafeEquals(mid.name, dep.name)) {
                    if (dep instanceof ModuleDependency) {
                        applies = !((ModuleDependency)dep).artifacts.findAll { DependencyArtifact dart ->
                            nullSafeEquals(dart.classifier, art.classifier)
                        }.empty
                    } else {
                        applies = true
                    }
                }

                applies
            }
            if (selected.empty)
                throw new GradleException("Can't find any artifacts that apply to dependency: ${dep.name}")

            return selected.first().file
        }

        private static Supplier<FileTree> addDependency(Project proj, NativeLib lib) {
            def config = lib.getConfiguration() ?: "native_$lib.name".toString()
            def cfg = proj.rootProject.configurations.maybeCreate(config)
            if (lib.getMaven() != null) {
                def dep = proj.rootProject.dependencies.add(config, lib.getMaven())
                return new FileTreeSupplier(cfg, { Set<ResolvedArtifact> artifacts ->
                    proj.rootProject.zipTree(resolve(artifacts, dep))
                } as Function<Set<ResolvedArtifact>, FileTree>)
            } else if (lib.getFile() != null && lib.getFile().directory) {
                // File is a directory
                return {
                    proj.rootProject.fileTree(lib.getFile())
                } as Supplier<FileTree>
            } else if (lib.getFile() != null && lib.getFile().file) {
                return {
                    proj.rootProject.zipTree(lib.getFile())
                } as Supplier<FileTree>
            } else {
                throw new GradleException("No target defined for dependency ${lib.name} (maven=${lib.getMaven()} file=${lib.getFile()})")
            }
        }

        private static FileCollection matcher(Project proj, Supplier<FileTree> tree, List<String> matchers, List<String> excludes) {
            return proj.files({
                tree.get().matching({ PatternFilterable filter ->
                    // <<!!ET_NOMATCH!!> is a magic string in the case the matchers are null.
                    // This is because, without include, the filter will include all files
                    // by default. We don't want this behavior.
                    filter.include(matchers ?: ["<<!!ET_NOMATCH!!>"])
                    filter.exclude(excludes ?: [] as List<String>)
                } as Action<PatternFilterable>)
            } as Callable<FileCollection>)
        }

        private static List<CombinedNativeLib> sortCombinedLibs(Iterable<CombinedNativeLib> libs) {
            List<SortUtils.TopoMember<CombinedNativeLib>> unsorted = libs.collect { CombinedNativeLib lib ->
                new SortUtils.TopoMember<CombinedNativeLib>(lib.name, lib.libs, lib);
            }
            return SortUtils.topoSort(unsorted).collect { it.extra }
        }

        private static ETNativeDepSet mergedDepSet(Project proj, DependencySpecExtension dse, String name, List<String> libNames,
                                                   Flavor flavor, BuildType buildType, NativePlatform platform) {
            List<ETNativeDepSet> libs = libNames.collect { dse.find(it, flavor, buildType, platform) }
            IDirectoryTree headers = libs.collect { it.headers }.inject { a, b -> a+b }
            IDirectoryTree sources = libs.collect { it.sources }.inject { a, b -> a+b }
            FileCollection linkFiles =  libs.collect { it.linkLibs }.inject { a, b -> a+b }
            FileCollection debugFiles = libs.collect { it.debugLibs }.inject { a, b -> a+b }
            FileCollection dynamicFiles = libs.collect { it.dynamicLibs }.inject { a, b -> a+b }
            List<String> systemLibs = libs.collectMany { it.systemLibs as Collection }

            return new ETNativeDepSet(
                    proj, name,
                    headers, sources, linkFiles,
                    dynamicFiles, debugFiles, systemLibs,
                    platform, flavor, buildType
            )
        }

        private static List<Flavor> getFlavors(BaseLibSpec lib, final FlavorContainer flavors) {
            if (lib.flavor == null && (lib.flavors == null || lib.flavors.empty))
                return [null] as List;
            return (lib.flavors ?: [lib.flavor] as List<String>).collect {
                flavors.findByName(it) as Flavor
            }.findAll { it != null }
        }

        private static List<BuildType> getBuildTypes(BaseLibSpec lib, final BuildTypeContainer buildTypes) {
            if (lib.buildType == null && (lib.buildTypes == null || lib.buildTypes.empty))
                return [null] as List;
            return (lib.buildTypes ?: [lib.buildType] as List<String>).collect {
                buildTypes.findByName(it) as BuildType
            }.findAll { it != null }
        }

        private static List<NativePlatform> getPlatforms(BaseLibSpec lib, final PlatformContainer platforms) {
            if (lib.targetPlatform == null && (lib.targetPlatforms == null || lib.targetPlatforms.empty))
                return [] as List;
            return (lib.targetPlatforms ?: [lib.targetPlatform] as List<String>).collect {
                platforms.findByName(it) as NativePlatform
            }.findAll { it != null }
        }
    }
}
