EmbeddedTools
====

Additions to the Gradle DSL for building and deploying to remote targets.

Multiple targets can be supported, as well as multiple deployers.

The plugin can be run with both Java and Native (C, C++, etc) projects, or neither if only files and commands are deployed. 

Commands:  
`gradlew deploy` will run the deploy steps for all deployers  
`gradlew deploy<deployer>` will run the deploy steps for the `<deployer>` deployer on all targets
`gradlew deploy<target>` will run the deploy steps for the `<target>` target on all deployers

Properties:  
`gradlew deploy -Pskip-cache` will skip the cache check and force redeployment of all files

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
                defineTools(it, "arm-prefix-", "-suffix")   // Defines tools for C, C++, Asm, Linkers and Archivers. Does not define Objective C/C++
            }
        }
    }
    // Define components as normal
}
```

## Model 

```gradle
import jaci.gradle.toolchains.*

// DeployableStep properties //
directory = 'mydir'     // Directory to use, relative to the scope prior
order = 50              // The order of this element with other elements. Lower numbers first. Default: 50
onlyIf = { execute('echo Hello') == 'Hello' }   // Only execute this step if the closure evaluates to true
precheck = { execute('echo precheck') }         // Execute closure before onlyIf
predeploy = { execute('echo predeploy') }       // Execute closure after onlyIf, but before deploy
postdeploy = { execute('echo postdeploy') }     // Execute closure after deploy

// DSL
deploy {
    targets {
        myTarget {
            addresses << "10.XX.XX.YY" << "myhost.local"    // Define the addresses used to search for this device
            async = true                // Check all addresses simultaneously. Default: true
            mkdirs = true               // Make directories during deploy. Default: true
            directory = '.'             // Root directory to deploy to, relative to user home dir. Default: .
            user = 'myuser'             // User to login as. Required.
            password = '**'             // Password to use for login. Default: ''
            promptPassword = true       // Optionally prompt for password. Overrides password field
            timeout = 3                 // Timeout before declaring the target unreachable in seconds. Default: 3
            failOnMissing = true        // Fail the build if the target can't be found. Default: true
        }
    }
    deployers {
        myDeployer {
            targets << 'myTarget'       // Set the targets this deployer responds to
            // Inherited from DeployableStep. See above. //
            fileArtifact('myFileArtifact') {
                // Inherited from DeployableStep. See above. //
                file = file('myfile.dat')   // The file to deploy
                filename = 'myfile2.dat'    // The filename to use. By default, it is the same name as file above
            }

            fileCollectionArtifact('myFileCollectionArtifact') {
                // Inherited from DeployableStep. See above. //
                files = tasks.jar.outputs.files`    // Set the files to use (in this case, the output jar file). Responds to FileCollection (e.g. FileTree, ZipTree, etc)
            }

            commandArtifact('myCommandArtifact') {
                // Inherited from DeployableStep. See above. //
                command = 'echo Hello'      // The command to run
                ignoreError = true          // Ignore if the command fails? Default: false
                // After the deploy task has run, you can access the 'result' property to obtain the command output.
            }

            nativeArtifact('myNativeArtifact') {
                // Inherited from fileArtifact. See above. //
                component = 'my_component'  // The name of the Native Component (in the model space) to deploy
                targetPlatform = 'crossArm' // The name of the Target Platform variant of the binary to deploy
            }
        }
    }
}

model {
    platforms {
        crossArm { operatingSystem 'linux'; architecture 'arm' }    // Add a new target platform for building
    }

    toolChains {
        crossGcc(CrossGcc) {
            target('crossArm') {
                defineTools(it, "arm-prefix-", "-suffix")   // Defines tools for C, C++, Asm, Linkers and Archivers. Does not define Objective C/C++
            }
        }
    }
}
```