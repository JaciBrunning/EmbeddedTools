package jaci.gradle

import groovy.swing.SwingBuilder
import jaci.gradle.deploy.DeployPlugin
import jaci.gradle.toolchains.ToolchainsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Service

class EmbeddedTools implements Plugin<Project> {
    void apply(Project project) {
        project.getPluginManager().apply(ToolchainsPlugin)
        project.getPluginManager().apply(DeployPlugin)
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
}