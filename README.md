EmbeddedTools
====

Additions to the Gradle DSL for building and deploying to remote targets.

Multiple targets can be supported, as well as multiple deployers.

The plugin can be run with both Java and Native (C, C++, etc) projects, or neither if only files and commands are deployed. 

Commands:  
`gradlew deploy` will run the deploy steps for all deployers  
`gradlew deploy<deployer>` will run the deploy steps for the `<deployer>` deployer on all targets

Properties:  
`gradlew deploy -Pdeploy-quiet` will suppress deploy task output  
`gradlew deploy -Pdeploy-dry` will do a dry run (will not connect to target, will only print commands run and files deployed)  
`gradlew deploy -Pdeploy-dirty` will force caches of all deployed files to be invalidated, forcing the files to be redeployed  

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
import jaci.gradle.deployers.*
import jaci.gradle.targets.*
import jaci.gradle.toolchains.*

model {
    targets {
        myTarget(RemoteTarget) {
            addresses << "10.0.0.1" << "mytarget"       // Addresses for the remote target. Can be IP addresses, hostnames
            asyncFind true                              // Search for each address in a new task for parallel execution. Makes finding target faster on multi-core systems. Default: true
            directory "/home/myuser"                    // Directory to deploy to. Default: user home directory
            mkdirs true                                 // Make directories when deploying. Default: true
            user "myuser"                               // User to login as. Required.
            password "secret"                           // Password to use for the user. Default: blank
            promptPassword true                         // Prompt for the password when running gradlew deploy? Default: false
            timeout 3                                   // Timeout in seconds when connecting to target. Default: 5
            failOnMissing true                          // Throw an exception when the target cannot be found. If false, target is simply skipped. Default: true
        }

        myOtherTarget(RemoteTarget) {
            addresses << "myothertarget"
            user "myotheruser"
        }
    }

    deployers {
        mydeployer(Deployer) {
            predeploy << "echo Hello World"             // Commands to run prior to deploying artifacts

            user "myotheruser"                          // User to login as. Default: target user
            password "secret"                           // Password to use. Default: blank
            promptPassword true                         // Prompt password? Default: false

            artifacts {
                myfile(FileArtifact) {                  // Set up a File artifact to deploy.
                    file "myfile.dat"                       // Set the file to deploy. Required.
                    filename "myfile.othername"             // Set the filename on the remote system. Default: name of file above
                    cache true                              // Cache file on remote system? Default: true

                    predeploy << "mycommand"                // Set commands to run before deploying this artifact
                    postdeploy << "mycommand"               // Set commands to run after deploying this artifact
                    order 1                                 // Set the order of this artifact. Smaller numbers deployed first. Default: 50
                }

                jar(JavaArtifact) {                     // Set up a Java artifact. Name of this artifact is the task name (jar for most projects)
                    filename "myfile.othername"             // Set the filename on the remote system. Default: name of java artifact file
                    cache true                              // Cache file on remote system? Default: true

                    predeploy << "mycommand"                // Set commands to run before deploying this artifact
                    postdeploy << "mycommand"               // Set commands to run after deploying this artifact
                    order 1                                 // Set the order of this artifact. Smaller numbers deployed first. Default: 50
                }

                myprogram(NativeArtifact) {             // Set up a Native (C, C++, etc) artifact. Name of this artifact is the name of the native component.
                    filename "myfile.othername"             // Set the filename on the remote system. Default: name of native artifact file
                    cache true                              // Cache file on remote system? Default: true

                    predeploy << "mycommand"                // Set commands to run before deploying this artifact
                    postdeploy << "mycommand"               // Set commands to run after deploying this artifact
                    order 1                                 // Set the order of this artifact. Smaller numbers deployed first. Default: 50

                    libraries true                          // Should shared libraries for this component be deployed? Default: true
                    librootdir "/usr/local/lib"             // Directory to deploy shared libraries to. Relative to artifact directory. Default: same directory as artifact
                    librarycache true                       // Should shared libraries be cached on the target? Default: true
                    platform "x64"                          // What target platform for the component should be deployed? Required.
                }

                mycommand(CommandArtifact) {            // Set up a Command artifact to deploy. 
                    directory "subdir"                      // Set the working directory for the command (relative to target directory. Absolute paths permitted)
                    command "echo Hello Command Artifact"   // Set the command to run
                }
            }

            postdeploy << "echo Goodbye World"               // Commands to run after deploying artifacts
            targets << "myTarget"                       // Targets this deployer will deploy to
            order 1                                     // Order of the target. Smaller numbers will deploy first. Default: 50
        }
    }

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