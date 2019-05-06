EmbeddedTools
====
Compile and Deploy for Embedded Targets in both Java and C++.

EmbeddedTools adds compiler and library rules to make writing native software easier.
For all projects, you can define deployment targets and artifacts. The deploy process works over SSH/SFTP and
is extremely quick.

Commands:
`gradlew deploy` will deploy all artifacts
`gradlew deploy<artifact name><target name>` will deploy only the specified artifact to the specified target

Properties:
`gradlew deploy -Pdeploy-dirty` will skip the cache check and force redeployment of all files
`gradlew deploy -Pdeploy-dry` will do a 'dry run' (will not connect or deploy to target, instead only printing to console)

## Installing plugin
Include the following in your `build.gradle`
```gradle
plugins {
    id "jaci.gradle.EmbeddedTools" version "<latest version>"
}
```

See [https://plugins.gradle.org/plugin/jaci.gradle.EmbeddedTools](https://plugins.gradle.org/plugin/jaci.gradle.EmbeddedTools) for the latest version

## Spec

```gradle
import jaci.gradle.toolchains.*
import jaci.gradle.nativedeps.*

// DSL (all properties optional unless stated as required)
deploy {
    targets {
        target('myTarget') {
            directory = '/home/myuser'  // The root directory to start deploying to. Default: user home
            maxChannels = 1         // The number of channels to open on the target (how many files / commands to run at the same time). Default: 1
            timeout = 3             // Timeout to use when connecting to target. Default: 3 (seconds)
            failOnMissing = true    // Should the build fail if the target can't be found? Default: true

            locations {
                ssh {
                    address = "mytarget.local"  // Required. The address to try
                    user = 'myuser'             // Required. The user to login as
                    password = ''               // The password for the user. Default: blank (empty) string
                    ipv6 = false                // Are IPv6 addresses permitted? Default: false
                }
            }
        }
    }

    artifacts {
        // COMMON PROPERTIES FOR ALL ARTIFACTS //
        all {
            directory = 'mydir'                     // Subdirectory to use. Relative to target directory
            targets << 'myTarget'                   // Targets to deploy to

            onlyIf = { execute('echo Hi').result == 'Hi' }   // Check closure for artifact. Will not deploy if evaluates to false

            predeploy << { execute 'echo Pre' }      // After onlyIf, but before deploy logic
            postdeploy << { execute 'echo Post' }    // After this artifact's deploy logic

            disabled = true                         // Disable this artifact. Default: false.

            dependsOn('someTask')                   // Make this artifact depend on a task
        }
        // END COMMON //

        fileArtifact('myFileArtifact') {
            file = file('myFile')               // Set the file to deploy. Required.
            filename = 'myFile.dat'             // Set the filename to deploy to. Default: same name as file
        }

        // FileCollectionArtifact is a flat collection of files - directory structure is not preserved
        fileCollectionArtifact('myFileCollectionArtifact') {
            files = fileTree(dir: 'myDir')      // Required. Set the filecollection (e.g. filetree, files, etc) to deploy
        }

        // FileTreeArtifact is like a FileCollectionArtifact, but the directory structure is preserved
        fileTreeArtifact('myFileTreeArtifact') {
            files = fileTree(dir: 'mydir')      // Required. Set the fileTree (e.g. filetree, ziptree) to deploy
        }

        commandArtifact('myCommandArtifact') {
            command = 'echo Hello'              // The command to run. Required.
            // Output will be stored in 'result' after execution
        }

        // JavaArtifact inherits from FileArtifact
        javaArtifact('myJavaArtifact') {
            jar = 'jar'                         // The jar (or Jar task) to deploy. Default: 'jar'
            // Note: This artifact will automatically depend on the jar build task
        }

        // NativeArtifact inherits from FileArtifact
        nativeArtifact('myNativeArtifact') {
            component = 'my_program'            // Required. The name of the native component (model.components {}) to deploy.
            targetPlatform = 'desktop'          // The name of the native platform (model.platforms {}) to deploy.

            // Note: This artifact will automatically depend on the native component link task
        }

        // NativeLibraryArtifact inherits from FileCollectionArtifact
        nativeLibraryArtifact('myNativeLibraryArtifact') {
            library = 'mylib'                   // Required. Name of library (model.libraries {}) to deploy.
            targetPlatform = 'desktop'          // The name of the native platform (model.platforms {}) to deploy.
            flavor = 'myFlavor'                 // The name of the flavor (model.flavors {}) to deploy.
            buildType = 'myBuildType'           // The name of the buildType (model.buildTypes {}) to deploy.
        }
    }
}

model {
    libraries {
        // COMMON PROPERTIES FOR ALL LIBRARIES //
        all {
            libraryName = 'myactuallib'     // The name to give this library in useLibrary and when referencing from other places.
            targetPlatform = 'desktop'      // The name of the native platform (model.platforms {}) this lib is built for
            targetPlatforms = ['desktop1', 'desktop2']  // Same as targetPlatform, but for multiple platforms.
            flavor = 'myFlavor'             // The name of the flavor (model.flavors {}) this lib is for
            buildType = 'myBuildType'       // The name of the buildType (model.buildTypes {}) this lib is for
        }
        // END COMMON

        mylib(NativeLib) {
            headerDirs << 'include'                         // Directories for headers
            sourceDirs << 'sources'                         // Directories for sources
            staticMatchers << '**/*.a'                      // Static Libraries to be linked at compile time
            sharedMatchers << '**/*.so'                     // Shared Libraries to be linked at compile time
            dynamicMatchers << '**/*.so'                    // Libraries that aren't linked, but still needed at runtime.
            systemLibs << 'm'                               // System libs to load with -l (provided by toolchain)

            maven = "some.maven.package:mylib:1.0.0@zip"    // Load from maven. Must be a zip or zip-compatible (like a jar)
            file = project.file("mylib.zip")                // Load from filesystem instead. Can be given a zip or a directory.
        }

        // You can create a collection of libraries using CombinedNativeLib
        myComboLib(CombinedNativeLib) {
            libs << 'myactuallib' << 'someotherlib'
        }
    }

    components {
        my_program(NativeExecutableSpec) {
            embeddedTools.useLibrary(it, "myComboLib")
        }
    }
}
```
