package jaci.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.platform.base.*
import org.gradle.nativeplatform.*

import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.*
import org.hidetake.groovy.ssh.connection.*

import java.security.MessageDigest
import java.nio.file.Files

import groovy.swing.SwingBuilder

class DeployerTask extends DefaultTask {
    @Input
    Deployer deployer

    @TaskAction
    void configureDeploy() {
        ext.deployer = deployer
    }
}

class ConfigureNativeBinaryTask extends DefaultTask {
    @Input
    File file

    @Input
    NativeBinarySpec spec

    @TaskAction
    void configureBinary() {
        ext.file = file
        ext.spec = spec
    }
}

class DetermineTargetAddressTask extends DefaultTask {
    @Input
    RemoteTarget target

    @TaskAction
    void determineAddress() {
        if (project.hasProperty("deploy-dry")) {
            println "-> DRY RUN: Using target address ${target.addresses[0]}"
        } else {
            ext.password = target.password == null ? "" : target.password
            if (target.promptPassword) {
                if (System.console() == null) {
                    // Using Gradle Daemon, try for a dialog box.
                    println "-> Could not prompt console password. Please enter password in dialog box:"
                    try {
                        new SwingBuilder().edt {
                            dialog(modal: true, title: "Password for ${target.name}", alwaysOnTop: true, resizable: false, locationRelativeTo: null, pack: true, show: true) {
                                vbox {
                                    label(text: "Enter Password for ${target.name}")
                                    input = passwordField()
                                    button(defaultButton: true, text: 'OK', actionPerformed: { ext.password = input.password; dispose() })
                                }
                            }
                        }
                    } catch (all) {
                        println "--> Could not spawn password dialog box (may be headless). Using default password (in build.gradle or blank if not set). Run with --no-daemon to prompt password"
                    }
                } else {
                    ext.password = new String(System.console().readPassword("\n-> Password for ${target.name}?:\n"))
                }
            }
            
            def runTest = { addr ->
                try {
                    def result = project.deploy_ssh.run {
                        // TODO: Set knownHosts policy
                        session(host: addr, user: target.user, password: ext.password, timeoutSec: target.timeout, knownHosts: AllowAnyHosts.instance) {
                            println "\t--> Success"
                        }
                    }
                    return true
                } catch (all) {
                    println "\t--> Fail"
                    return false
                }
            }

            def foundTarget = target.addresses.any { addr ->
                println "-> Attempting target address: ${addr}"
                if (runTest(addr)) {
                    ext.address = addr
                    return true
                }
                return false
            }
            ext.foundTarget = foundTarget
            if (target.failOnMissing && !foundTarget) {
                throw new DeployException("Target ${target.name} could not be found! ${target.name}.failOnMissing is set.")
            }
        }
    }
}

class DeployTargetTask extends DefaultTask {
    @Input
    RemoteTarget target

    @Input
    String addrTaskName

    @TaskAction
    void deploy() {
        def dryrun = project.hasProperty("deploy-dry")
        def skipcache = project.hasProperty("deploy-dirty")

        def printmsg = project.hasProperty("deploy-quiet") ? { msg -> } : { msg -> println msg }

        if (dryrun) {
            runDeploy({ cmd, workingdir ->
                printmsg "-C-> ${cmd} @ ${workingdir}"
            }, { filesrc, filedst, workingdir, cache ->
                printmsg "-F-> (${cache && !skipcache ? 'CACHE' : 'NO CACHE'}) ${filesrc} ==> ${filedst}"
            })
        } else {
            def addrTask = project.tasks.findByName(addrTaskName)
            if (!addrTask.foundTarget) {
                println "-> Skipping target ${target.name} (could not find target)"
                return
            }
            def address = addrTask.address
            def password = addrTask.password

            project.deploy_ssh.run {
                session(host: address, user: target.user, password: password, timeoutSec: target.timeout, knownHosts: AllowAnyHosts.instance) {
                    def cachecheck = !skipcache
                    try {
                        def sum = execute "echo test | md5sum"
                        if (!sum.split(" ")[0].equalsIgnoreCase("d8e8fca2dc0f896fd7cb4cb0031ba249")) {
                            printmsg "-> md5sum not producing expected output on remote target. Skipping cache checks..."
                            cachecheck = false
                        }
                    } catch (all) {
                        printmsg "-> md5sum not supported on remote target. Skipping cache checks..."
                        cachecheck = false
                    }

                    runDeploy({ cmd, workingdir ->
                        if (target.mkdirs) execute "mkdir -p ${workingdir}"
                        
                        printmsg "-C-> ${cmd} @ ${workingdir}"
                        def result = execute([ "cd ${workingdir}", cmd ].join('\n'))
                        printmsg "------> ${result}"

                    }, { filesrc, filedst, workingdir, cache ->
                        if (target.mkdirs) execute "mkdir -p ${workingdir}"

                        def toDeploy = true
                        if (cache && cachecheck) {
                            def remote_md5 = execute("md5sum ${filedst} 2> /dev/null || true").split(" ")[0]
                            def md = MessageDigest.getInstance("MD5")
                            md.update(Files.readAllBytes(filesrc.toPath()))
                            def local_md5 = md.digest().encodeHex().toString()

                            if (remote_md5.equals(local_md5)) toDeploy = false
                        }

                        if (toDeploy) {
                            printmsg "-F->${cache && cachecheck ? " (OUT OF DATE)" : ""} ${filedst}"
                            put from: filesrc, into: filedst
                        }
                    })
                }
            }
        }
    }

    void runDeploy(cmd_callback, file_callback) {
        def rootDirectory = target.directory == null ? "." : target.directory

        def deployerList = []
        // Find all deployers that will be bound to this target
        project.tasks.findAll { this in it.finalizedBy.getDependencies() }.forEach { deployerTask -> 
            deployerList << deployerTask.ext.deployer
        }
        deployerList = deployerList.toSorted { a, b -> a.getOrder() <=> b.getOrder() }

        deployerList.forEach { deployer -> 
            deployer.getPredeploy().forEach { cmd ->
                cmd_callback(cmd, rootDirectory)
            } 

            deployer.getArtifacts().toSorted { a, b -> a.getOrder() <=> b.getOrder() }.forEach { artifact ->
                def deployDirectory = (artifact.directory != null) ? DeployTools.join(rootDirectory, artifact.directory) : rootDirectory

                if (artifact.getPredeploy() != null) artifact.getPredeploy().forEach { cmd ->
                    cmd_callback(cmd, deployDirectory)
                }

                def customFilename = (artifact instanceof FileArtifact) ? artifact.getFilename() : null
                deployDirectory = DeployTools.normalize(deployDirectory)
                def deploySourceFile = null
                def deployTargetFile = customFilename == null ? null : DeployTools.join(deployDirectory, customFilename)

                if (artifact instanceof JavaArtifact) {
                    def task = project.tasks.findByName(artifact.name)
                    if (task != null) {
                        def files = task.outputs.files.files
                        if (files != null && !files.isEmpty()) {
                            def file = files[0] 
                            def filename = customFilename == null ? file.name : customFilename
                            deploySourceFile = file
                            deployTargetFile = (deployTargetFile == null) ? DeployTools.join(deployDirectory, filename) : deployTargetFile
                        }
                    }
                } else if (artifact instanceof NativeArtifact) {
                    def taskname = "configureDeployArtifact${deployer.name.capitalize()}${artifact.name.capitalize()}"
                    def task = project.tasks.findByName(taskname)
                    if (task != null) {
                        def file = task.file
                        def filename = customFilename == null ? file.name : customFilename
                        deploySourceFile = file
                        deployTargetFile = (deployTargetFile == null) ? DeployTools.join(deployDirectory, filename) : deployTargetFile

                        if (artifact.libraries) {
                            def libDeployDir = artifact.librootdir == null ? deployDirectory : DeployTools.normalize(DeployTools.join(deployDirectory, artifact.librootdir))
                            task.spec.getLibs().forEach { lib -> 
                                lib.getRuntimeFiles().files.forEach { libfile ->
                                    if (libfile.exists()) {
                                        def libTargetFile = DeployTools.join(libDeployDir, libfile.name)
                                        file_callback(libfile, libTargetFile, libDeployDir, artifact.librarycache)
                                    }
                                }
                            }
                        }
                    }
                } else if (artifact instanceof FileArtifact) {
                    def file = artifact.getFile()
                    def filename = customFilename == null ? file.name : customFilename
                    deploySourceFile = file
                    deployTargetFile = (deployTargetFile == null) ? DeployTools.join(deployDirectory, filename) : deployTargetFile
                } else if (artifact instanceof CommandArtifact) {
                    cmd_callback(artifact.getCommand(), deployDirectory)
                }
                
                if (deploySourceFile != null && deployTargetFile != null) {
                    deployTargetFile = DeployTools.normalize(deployTargetFile)
                    if (deploySourceFile.exists())
                    file_callback(deploySourceFile, deployTargetFile, deployDirectory, artifact.cache)
                }

                if (artifact.getPostdeploy() != null) artifact.getPostdeploy().forEach { cmd ->
                    cmd_callback(cmd, deployDirectory)
                }
            }

            deployer.getPostdeploy().forEach { cmd -> 
                cmd_callback(cmd, rootDirectory)
            }
        }
    }
}