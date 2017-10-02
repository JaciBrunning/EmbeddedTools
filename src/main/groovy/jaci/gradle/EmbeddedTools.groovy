package jaci.gradle

import jaci.gradle.deployers.*
import jaci.gradle.targets.*
import jaci.gradle.toolchains.*

import org.gradle.api.*
import org.gradle.model.*
import org.gradle.platform.base.*
import org.gradle.nativeplatform.*
import groovy.util.*

import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Service

import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext

import groovy.swing.SwingBuilder

class EmbeddedTools implements Plugin<Project> {
    void apply(Project project) {
        project.getPluginManager().apply(ToolchainsPlugin.class)
        getSsh()
    }

    static Service ssh_service
    static OutputEventListenerBackedLoggerContext ssh_defaultContext
    static Service getSsh() {
        if (ssh_service == null) {
            ssh_service = Ssh.newService()

            def logfield = ssh_service.log.class.getDeclaredField("context")
            logfield.setAccessible(true)

            ssh_defaultContext = logfield.get(ssh_service.log)
        }
        return ssh_service
    }

    static void silenceSsh() {
        def logfield = ssh_service.log.class.getDeclaredField("context")
        logfield.setAccessible(true)

        def ctx = new OutputEventListenerBackedLoggerContext({ text -> return }, { text -> return }, org.gradle.internal.time.Time.clock())
        ctx.setOutputEventListener({ event -> return })
            
        logfield.set(ssh_service.log, ctx)
    }

    static void unsilenceSsh() {
        def logfield = ssh_service.log.class.getDeclaredField("context")
        logfield.setAccessible(true)

        logfield.set(ssh_service.log, ssh_defaultContext)
    }

    static String join(String root, String relative) {
        if (relative.startsWith("/")) return relative;
        if (root.charAt(root.length() - 1) != "/") root += "/"
        root += relative;
    }

    static String normalize(String filepath) {
        def strings = filepath.split("/") as List
        def s = [] as Stack
        strings.forEach { str -> 
            if (str.trim().equals("..")) {
                s.pop()
            } else s.push(str)
        }
        return s.join("/")
    }

    static String promptPassword(String user) {
        def password = null
        if (System.console() == null) {
            // Using Gradle Daemon, try for a dialog box.
            println "-> Could not prompt console password. Please enter password in dialog box:"
            try {
                new SwingBuilder().edt {
                    dialog(modal: true, title: "Password for user ${user}", alwaysOnTop: true, resizable: false, locationRelativeTo: null, pack: true, show: true) {
                        vbox {
                            label(text: "Enter Password for user ${user}")
                            input = passwordField()
                            button(defaultButton: true, text: 'OK', actionPerformed: { password = input.password; dispose() })
                        }
                    }
                }
            } catch (all) {
                println "--> Could not spawn password dialog box (may be headless). Using default password (in build.gradle or blank if not set). Run with --no-daemon to prompt password"
            }
        } else {
            password = new String(System.console().readPassword("\n-> Password for user ${user}?:\n"))
        }
        return password
    }

    static class DeployRules extends RuleSource {
        @Model("targets")
        void createTargetsModel(TargetsSpec spec) { }

        @Defaults 
        void setDefaultTargetValues(@Each RemoteTarget target) {
            target.setAddresses([])
            target.setAsyncFind(true)
            target.setMkdirs(true)
            target.setPassword("")
            target.setPromptPassword(false)
            target.setTimeout(5)
            target.setFailOnMissing(true)
        }

        @Model("deployers")
        void createDeployersModel(DeployersSpec spec) { }

        @Defaults
        void setDefaultsDeployableStep(@Each DeployableStep deployableStep) {
            deployableStep.setPredeploy([])
            deployableStep.setPostdeploy([])
        }

        @Defaults
        void setDefaultDeployerValues(@Each Deployer deployer) {
            deployer.setOrder(50)
            deployer.setUser(null)
            deployer.setPassword("")
            deployer.setPromptPassword(false)
        }

        @Defaults
        void setDefaultArtifactValues(@Each ArtifactBase artifact) {
            artifact.setOrder(50)
        }

        @Defaults
        void setDefaultFileArtifactValues(@Each FileArtifact artifact) {
            artifact.setCache(true)
        }

        @Defaults
        void setDefaultNativeArtifactValues(@Each NativeArtifact artifact) {
            artifact.setLibraries(true)
            artifact.setLibrarycache(true)
        }

        @Mutate
        void createDeployerTasks(final ModelMap<Task> tasks, final DeployersSpec deployers, final TargetsSpec targets) {
            targets.forEach { target ->
                def targetAddrTaskName = "determineTargetAddr${target.name.capitalize()}"
                tasks.create(targetAddrTaskName, DetermineTargetAddressTask) { task ->
                    task.target = target
                }

                tasks.create('deployTarget' + target.name.capitalize(), DeployTargetTask) { task ->
                    task.target = target
                    task.addrTaskName = targetAddrTaskName
                    task.dependsOn targetAddrTaskName
                }
            }

            deployers.forEach { deployer ->
                tasks.create('deploy' + deployer.name.capitalize(), DeployerTask) { task ->
                    task.group 'DeployTools'
                    task.description "Run ${deployer.name} deployer for all artifacts"

                    task.deployer = deployer

                    targets.findAll { target -> target.name in deployer.targets }.forEach { target ->
                        task.finalizedBy('deployTarget' + target.name.capitalize())
                    }
                }
            }

            tasks.create('deploy', DefaultTask) { deployTask -> 
                deployTask.group "DeployTools"
                deployTask.description "Run all registered deployers"

                deployers.forEach { deployer ->
                    deployTask.dependsOn('deploy' + deployer.name.capitalize())
                }
            }
        }

        @Mutate
        void createBinariesTasks(final ModelMap<Task> tasks, final DeployersSpec deployers, BinaryContainer binaries) {
            deployers.forEach { deployer ->
                deployer.artifacts.forEach { artifact ->
                    if (artifact instanceof NativeArtifact) {
                        binaries.forEach { spec -> 
                            if ((spec instanceof NativeExecutableBinarySpec || spec instanceof SharedLibraryBinarySpec) && spec.component.name == artifact.name) {
                                if (artifact.platform == null || artifact.platform == spec.targetPlatform.name) {
                                    def file = (spec instanceof NativeExecutableBinarySpec) ? spec.executable.file : spec.sharedLibraryFile
                                    def taskname = "configureDeployArtifact${deployer.name.capitalize()}${artifact.name.capitalize()}"

                                    try {
                                        tasks.create(taskname, ConfigureNativeBinaryTask) { task ->
                                            task.file = file
                                            task.spec = spec
                                        }
                                    } catch(Exception e) {
                                        // Task already exists, ignore
                                    }
                                    if (artifact.libraries) {
                                        // spec.component.sources.forEach { src ->
                                        //     src.libs.forEach { lib ->
                                        //         if (lib.linkage == "shared") {
                                        //             println lib
                                        //         }
                                        //     }
                                        // }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}