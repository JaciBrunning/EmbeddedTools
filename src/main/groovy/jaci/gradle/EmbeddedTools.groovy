package jaci.gradle

import com.jcraft.jsch.JSch
import groovy.swing.SwingBuilder
import jaci.gradle.deploy.DeployPlugin
import jaci.gradle.nativedeps.NativeDepsPlugin
import jaci.gradle.toolchains.ToolchainsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class EmbeddedTools implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('em_projectwrapper', ProjectWrapper, project)
        project.getPluginManager().apply(ToolchainsPlugin)
        project.getPluginManager().apply(DeployPlugin)
        project.getPluginManager().apply(NativeDepsPlugin)
    }

    static class ProjectWrapper {
        Project project
        ProjectWrapper(Project project) { this.project = project }
    }

    static JSch jsch
    static JSch getJsch() {
        if (jsch == null) jsch = new JSch()
        return jsch
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

    static boolean isDryRun(Project project) {
        return project.hasProperty('deploy-dry')
    }

    static boolean isSkipCache(Project project) {
        return project.hasProperty('deploy-dirty')
    }
}