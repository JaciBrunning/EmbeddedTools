package jaci.gradle

import org.gradle.api.*
import org.gradle.model.*
import org.gradle.platform.base.*
import org.gradle.nativeplatform.*
import groovy.util.*

class Toolchains {
    static void apply(Project project) {
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

        project.ext.defineTools = { prefix, suffix ->

        }

        project.ext.defaultToolchains = {
            visualCpp(VisualCpp) {
                if (OperatingSystem.current().isWindows()) {
                    // Taken from nt-core, fixes VS2015 compilation issues 

                    // Workaround for VS2015 adapted from https://github.com/couchbase/couchbase-lite-java-native/issues/23
                    def VS_2015_INCLUDE_DIR = "C:/Program Files (x86)/Windows Kits/10/Include/10.0.10240.0/ucrt"
                    def VS_2015_LIB_DIR = "C:/Program Files (x86)/Windows Kits/10/Lib/10.0.10240.0/ucrt"
                    def VS_2015_INSTALL_DIR = 'C:/Program Files (x86)/Microsoft Visual Studio 14.0'
                    def vsInstallDir = file(VS_2015_INSTALL_DIR)

                    eachPlatform {
                        cppCompiler.withArguments { args ->
                            if (file(VS_2015_INCLUDE_DIR).exists()) {
                                args << "/I$VS_2015_INCLUDE_DIR"
                            }
                        }
                        linker.withArguments { args ->
                            if (file(VS_2015_LIB_DIR).exists()) {
                                if (platform.architecture.name == 'x86') {
                                    args << "/LIBPATH:$VS_2015_LIB_DIR/x86"
                                } else {
                                    args << "/LIBPATH:$VS_2015_LIB_DIR/x64"
                                }
                            }
                        }
                    }
                }
            }

            gcc(Gcc) { }

            clang(Clang) { }
        }
    }
}