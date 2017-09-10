package jaci.gradle

import org.gradle.api.*
import org.gradle.model.*
import org.gradle.platform.base.*
import org.gradle.nativeplatform.*
import org.gradle.nativeplatform.toolchain.*
import groovy.util.*

class ToolchainsPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.ext.defaultPlatforms = {
            project.model {
                platforms {
                    'any-32' {
                        architecture "x86"
                    }
                    'any-64' {
                        architecture "x86_64"
                    }
                    'arm' {
                        operatingSystem "linux"
                        architecture "arm"
                    }
                }
            }
        }

        project.ext.defineTools = { target, prefix, suffix ->
            target.cppCompiler.executable       = prefix + target.cppCompiler.executable + suffix
            target.cCompiler.executable         = prefix + target.cCompiler.executable + suffix
            target.linker.executable            = prefix + target.linker.executable + suffix
            target.assembler.executable         = prefix + target.assembler.executable + suffix
            target.staticLibArchiver.executable = prefix + target.staticLibArchiver.executable + suffix
        }

        project.ext.defaultToolchains = {
            project.model {
                toolChains {
                    visualCpp(VisualCpp) { }
                    gcc(Gcc) { }
                    clang(Clang) { }
                }
            }
        }
    }

    static class ToolchainRules extends RuleSource {
        @Mutate
        void configureOptionalBuildables(BinaryContainer binaries) {
            binaries.withType(NativeBinarySpec) { bin ->
                println "${bin} ${bin.toolChain}"
            }
        }
    }
}