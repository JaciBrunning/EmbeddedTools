package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.platform.NativePlatform

@CompileStatic
public class DependencySpecExtension {
    List<ETNativeDepSet> sets
    final Project project

    public DependencySpecExtension(Project project) {
        sets = [] as List<ETNativeDepSet>
        this.project = project
    }

    public ETNativeDepSet find(String name, NativeBinarySpec binary) {
        return find(name, binary.flavor, binary.buildType, binary.targetPlatform)
    }

    public ETNativeDepSet find(String name, Flavor flavor, BuildType buildType, NativePlatform targetPlatform) {
        return sets.find { ETNativeDepSet set ->
            set.getName().equals(name) && set.appliesTo(flavor, buildType, targetPlatform)
        }
    }
}
