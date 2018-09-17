package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.SortUtils
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
import org.gradle.language.nativeplatform.DependentSourceSet
import org.gradle.model.*
import org.gradle.nativeplatform.*
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.tasks.AbstractLinkTask
import org.gradle.platform.base.BinaryTasks
import org.gradle.platform.base.PlatformContainer

import java.util.concurrent.Callable
import java.util.function.Supplier

@CompileStatic
class NativeDepsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        DependencySpecExtension dse = project.extensions.create("ETDependencySpecs", DependencySpecExtension, project)

        project.extensions.add("useLibrary", { Object closureArg, String... names ->
            if (closureArg in TargetedNativeComponent) {
                TargetedNativeComponent component = (TargetedNativeComponent)closureArg
                component.binaries.withType(NativeBinarySpec).all { NativeBinarySpec bin ->
                    Set<DelegatedDependencySet> dds = names.collect { String name ->
                        new DelegatedDependencySet(name, bin, dse)
                    } as Set

                    bin.inputs.withType(DependentSourceSet) { DependentSourceSet dss ->
                        dds.each { DelegatedDependencySet set ->
                            dss.lib(set)
                        }
                    }
                }
            } else if (closureArg in NativeBinarySpec) {
                NativeBinarySpec bin = (NativeBinarySpec) closureArg
                Set<DelegatedDependencySet> dds = names.collect { String name ->
                    new DelegatedDependencySet(name, bin, dse)
                } as Set

                bin.inputs.withType(DependentSourceSet) { DependentSourceSet dss ->
                    dds.each { DelegatedDependencySet set ->
                        dss.lib(set)
                    }
                }
            } else if (closureArg in LanguageSourceSet) {
                throw new GradleException('The useLibrary command needs to be placed directly in the component. Move it outside of the sources declaration.')
            } else {
                throw new GradleException('Unknown type for useLibrary target. You put this declaration in a weird place...')
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

                FileCollection sharedFiles = matcher(project, rootTree, lib.sharedMatchers)
                FileCollection staticFiles = matcher(project, rootTree, lib.staticMatchers)
                FileCollection dynamicFiles = matcher(project, rootTree, lib.dynamicMatchers)

                IDirectoryTree headerFiles = new DefaultDirectoryTree(rootTree, lib.headerDirs ?: [] as List<String>)
                IDirectoryTree sourceFiles = new DefaultDirectoryTree(rootTree, lib.sourceDirs ?: [] as List<String>)

                targetPlatforms.each { NativePlatform platform ->
                    ETNativeDepSet depSet = new ETNativeDepSet(
                        project, libName,
                        headerFiles, sourceFiles,
                        staticFiles, sharedFiles, dynamicFiles,
                        lib.systemLibs ?: [] as List<String>,
                        platform, flavor, buildType
                    )
                    dse.sets.add(depSet)
                }
                null
            }

            sortCombinedLibs(spec.withType(CombinedNativeLib)).each { CombinedNativeLib lib ->
                def uniqName = lib.name
                def libName = lib.libraryName ?: uniqName

                Flavor flavor = lib.flavor == null ? null : flavors.findByName(lib.flavor)
                BuildType buildType = lib.buildType == null ? null : buildTypes.findByName(lib.buildType)
                List<NativePlatform> targetPlatforms = getPlatforms(lib, platforms)

                targetPlatforms.each { NativePlatform platform ->
                    ETNativeDepSet dep = mergedDepSet(project, dse, libName, lib.libs, flavor, buildType, platform)
                    dse.sets.add(dep)
                }
                null
            }
        }

        @BinaryTasks
        void addLinkerArgs(ModelMap<Task> tasks, final NativeBinarySpec binary) {
            // We can't use binary.libs because that forces a reenumeration of all libraries,
            // which breaks multiproject builds. Instead, we have to resolve manually
            binary.inputs.withType(DependentSourceSet) { DependentSourceSet dss ->
                dss.libs.each { Object lib ->
                    if (lib instanceof DelegatedDependencySet) {
                        DelegatedDependencySet set = (DelegatedDependencySet)lib
                        tasks.withType(AbstractLinkTask) { AbstractLinkTask linkTask ->
                            linkTask.linkerArgs.addAll(new DefaultProvider<List<String>>({
                                set.getSystemLibs().collectMany { name -> ["-l", name] as Collection<String> }
                            }))
                        }
                    }
                }
            }
        }

        private static boolean nullSafeEquals(Object a, Object b) {
            return (a == b) || (a != null && b != null && a.equals(b))
        }

        private static File resolve(Configuration cfg, Dependency dep) {
            def artifacts = cfg.resolvedConfiguration.resolvedArtifacts
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
            def cfg = proj.configurations.maybeCreate(config)
            if (lib.getMaven() != null) {
                def dep = proj.dependencies.add(config, lib.getMaven())
                return {
                    proj.zipTree(resolve(cfg, dep))
                } as Supplier<FileTree>
            } else if (lib.getFile() != null && lib.getFile().directory) {
                // File is a directory
                return {
                    proj.fileTree(lib.getFile())
                } as Supplier<FileTree>
            } else if (lib.getFile() != null && lib.getFile().file) {
                return {
                    proj.zipTree(lib.getFile())
                } as Supplier<FileTree>
            } else {
                throw new GradleException("No target defined for dependency ${lib.name} (maven=${lib.getMaven()} file=${lib.getFile()})")
            }
        }

        private static FileCollection matcher(Project proj, Supplier<FileTree> tree, List<String> matchers) {
            return proj.files({
                tree.get().matching({ PatternFilterable filter ->
                    // <<!!ET_NOMATCH!!> is a magic string in the case the matchers are null.
                    // This is because, without include, the filter will include all files
                    // by default. We don't want this behavior.
                    filter.include(matchers ?: ["<<!!ET_NOMATCH!!>"])
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
            FileCollection staticFiles = libs.collect { it.staticLibs }.inject { a, b -> a+b }
            FileCollection sharedFiles = libs.collect { it.sharedLibs }.inject { a, b -> a+b }
            FileCollection dynamicFiles = libs.collect { it.dynamicLibs }.inject { a, b -> a+b }
            List<String> systemLibs = libs.collectMany { it.systemLibs as Collection }

            return new ETNativeDepSet(
                    proj, name,
                    headers, sources,
                    staticFiles, sharedFiles, dynamicFiles,
                    systemLibs,
                    platform, flavor, buildType
            )
        }

        private static List<NativePlatform> getPlatforms(BaseLibSpec lib, final PlatformContainer platforms) {
            if (lib.targetPlatform == null && (lib.targetPlatforms == null || lib.targetPlatforms.empty))
                return [] as List;
            return (lib.targetPlatforms ?: [lib.targetPlatform] as List<String>).collect {
                platforms.getByName(it) as NativePlatform
            }
        }
    }
}
