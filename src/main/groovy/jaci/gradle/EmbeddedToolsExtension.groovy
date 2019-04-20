package jaci.gradle

import javax.inject.Inject

import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.platform.base.VariantComponentSpec

import jaci.gradle.nativedeps.DelegatedDependencySet
import jaci.gradle.nativedeps.DependencySpecExtension

@CompileStatic
class EmbeddedToolsExtension {
    private DependencySpecExtension dse = null
    private Project project

    @Inject
    EmbeddedToolsExtension(Project project) {
        this.project = project
    }

    void useLibrary(VariantComponentSpec component, boolean skipOnUnknown, String... libraries) {
        component.binaries.withType(NativeBinarySpec).all { binary ->
            useLibrary(binary, skipOnUnknown, libraries)
        }
    }

    void useLibrary(VariantComponentSpec component, String... libraries) {
        useLibrary(component, false, libraries)
    }

    void useLibrary(NativeBinarySpec binary, boolean skipOnUnknown, String... libraries) {
        if (dse == null) {
            dse = project.extensions.getByType(DependencySpecExtension)
        }
        libraries.each { library ->
            binary.lib(new DelegatedDependencySet(library, binary, dse, skipOnUnknown))
        }
    }

    void useLibrary(NativeBinarySpec binary, String... libraries) {
        useLibrary(binary, false, libraries)
    }
}
