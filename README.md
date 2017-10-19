EmbeddedTools
====
Compile and Deploy for Embedded Targets in both Java and C++. 

EmbeddedTools adds compiler and library rules to make writing native software easier.
For all projects, you can define deployment targets and artifacts. The deploy process works over SSH/SFTP and
is extremely quick.

Commands:   
`gradlew deploy` will deploy all artifacts  
`gradlew deploy<artifact name>` will deploy only the specified artifact  

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

## Cross GCC
EmbeddedTools adds a new toolchain type, `CrossGcc`, with the purpose for building on embedded platforms. Furthermore, all C, C++, Asm, ObjC and ObjC++ binary components
are optional builds, meaning the build will continue even if there is no toolchain present.

```gradle
model {
    platforms {
        crossArm { operatingSystem 'linux'; architecture 'arm' }    // Add a new target platform for building
    }

    toolChains {
        crossGcc(CrossGcc) {
            target('crossArm') {
                defineTools(it, "arm-prefix-", "-suffix")   // Defines tools for C, C++, Asm, Linkers and Archivers. Does not define Objective C
            }
        }
    }
    // Define components as normal
}
```

## Spec 

```gradle
import jaci.gradle.toolchains.*
import jaci.gradle.nativedeps.*

// DSL (all properties optional unless stated as required)
deploy {
    targets {
        target('myTarget') {
            addresses << '172.22.11.2' << 'mydevice'        // Addresses to attempt to deploy to, in order of preference
            mkdirs = true           // Make directories on the remote device when deploying. Default: true 
            directory = 'mydir'     // The subdirectory on the target to deploy to. Default: SSH Default
            user = 'myuser'         // User to login as. Required.
            password = '***'        // Password to use. Default: blank
            promptPassword = true   // Should EmbeddedTools prompt for a password? Default: false. Overrides password above.
            timeout = 3             // Timeout to use when connecting to target. Default: 3 (seconds)
            failOnMissing = true    // Should the build fail if the target can't be found? Default: true
        }
    }
    artifacts {
        // COMMON PROPERTIES FOR ALL ARTIFACTS //
        directory = 'mydir'                     // Subdirectory to use. Relative to target directory
        targets << 'myTarget'                   // Targets to deploy to

        precheck = { execute 'pwd' }            // Closure to execute before onlyIf
        onlyIf = { execute 'echo Hi' == 'Hi' }  // Check closure for artifact. Will not deploy if evaluates to false
        predeploy = { execute 'echo Pre' }      // After onlyIf, but before deploy logic
        postdeploy = { execute 'echo Post' }    // After this artifact's deploy logic

        after('someOtherArtifact')              // Make this artifact depend on another artifact
        dependsOn('someTask')                   // Make this artifact depend on a task
        // END COMMON //

        fileArtifact('myFileArtifact') {
            file = file('myFile')               // Set the file to deploy. Required.
            filename = 'myFile.dat'             // Set the filename to deploy to. Default: same name as file
        }

        fileCollectionArtifact('myFileCollectionArtifact') {
            files = fileTree(dir: 'myDir')      // Set the filecollection (e.g. filetree, files, etc) to deploy. Required
        }

        commandArtifact('myCommandArtifact') {
            command = 'echo Hello'              // The command to run. Required.
            // Output will be stored in 'result' after execution
        }

        javaArtifact('myJavaArtifact') {
            jar = 'jar'                         // The jar (or Jar task) to deploy. Required. (usually 'jar')
            filename = 'myjar.jar'              // Set the filename to deploy to. Default: same name as the generated jar
            // Note: This artifact will automatically depend on the jar build task
        }

        nativeArtifact('myNativeArtifact') {
            component = 'my_program'            // The name of the native component (model.components {}) to deploy. Required. Either shared library or executable
            targetPlatform = 'crossGcc'         // The name of the native platform (model.platforms {})) to deploy.
            filename = 'myProgram'              // Set the filename to deploy to. Default: same name as component generated file
            // Note: This artifact will automatically depend on the native component link task
        }

        nativeLibraryArtifact('myNativeLibraryArtifact') {
            library = 'mylib'                   // Name of library (model.libraries {}) to deploy. Required.
            matchers << '**/*.so'               // Matcher of library files to deploy to target.
        }
    }
}

model {
    libraries {
        mylib(NativeLib) {
            targetPlatform 'crossArm'           // Set the Target Platform for this library. Optional.
            flavor 'default'                    // Set the Flavor for this library. Optional.
            buildType 'debug'                   // Set the Build Type for this library. Optional.

            // One of the following
            file 'mydir'                                // Select a directory including the headers and compiled library files 
            file 'myfile.zip'                           // Select a zipfile including the headers and compiled library files
            maven 'mygroup:myartifact:myversion@zip'    // Select a maven artifact (zip file) including the headers and compiled library files

            sharedMatchers << '**/*.so'                 // The search pattern for shared libraries (to be added as -L flag)
            staticMatchers << '**/*.a'                  // The search pattern for static libraries (to be added as -L flag)
            libraryMatchers << '**/*.so'                // The search pattern for libraries to be deployed (if added in artifact), and linked
            libraryNames << 'customlib'                 // Manually add -l libraries.
            headerDirs << 'include'                     // The directories for headers of this library
        }

        myComboLib(CombinedNativeLib) {
            targetPlatform 'crossArm'           // Set the Target Platform for this library. Optional.
            flavor 'default'                    // Set the Flavor for this library. Optional.
            buildType 'debug'                   // Set the Build Type for this library. Optional.

            libs << 'mylib'                     // Set the libraries used in this combination lib
        }
    }

    platforms {
        crossArm { operatingSystem 'linux'; architecture 'arm' }    // Add a new target platform for building
    }

    toolChains {
        crossGcc(CrossGcc) {
            target('crossArm') {
                defineTools(it, "arm-prefix-", "-suffix")   // Defines tools for C, C++, Asm, Linkers and Archivers. Does not define Objective C
            }
        }
    }

    components {
        my_program(NativeExecutableSpec) {
            targetPlatform "crossArm"
            sources.cpp {
                lib library: "myComboLib"       // or lib library: 'mylib'
            }
        }
    }
}
```