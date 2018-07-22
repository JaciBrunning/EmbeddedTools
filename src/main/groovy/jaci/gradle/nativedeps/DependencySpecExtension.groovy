package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.platform.NativePlatform

@CompileStatic
public class DependencySpecExtension {
    List<ETNativeDepSet> sets

    public DependencySpecExtension() {
        sets = [] as List<ETNativeDepSet>
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
