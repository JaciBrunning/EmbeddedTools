package jaci.gradle

import com.jcraft.jsch.JSch
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployPlugin
import jaci.gradle.log.ETLoggerFactory
import jaci.gradle.nativedeps.NativeDepsPlugin
import jaci.gradle.toolchains.ToolchainsPlugin
import org.apache.log4j.Logger
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.nativeplatform.plugins.NativeComponentPlugin

@CompileStatic
class EmbeddedTools implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('em_projectwrapper', ProjectWrapper, project)

        ETLoggerFactory.INSTANCE.addColorOutput(project)

        project.getPluginManager().apply(DeployPlugin)

        // Only apply the ToolchainsPlugin and NativeDepsPlugin if we're building a native project.
        project.getPlugins().withType(NativeComponentPlugin).all { NativeComponentPlugin plugin ->
            Logger.getLogger(this.class).info("Native Project detected: ${plugin.class.name}".toString())
            project.getPluginManager().apply(ToolchainsPlugin)
            project.getPluginManager().apply(NativeDepsPlugin)
        }
    }

    @CompileStatic
    @Canonical
    static class ProjectWrapper {
        Project project
    }

    static JSch jsch
    static JSch getJsch() {
        if (jsch == null) jsch = new JSch()
        return jsch
    }

    static boolean isDryRun(Project project) {
        return project.hasProperty('deploy-dry')
    }

    static boolean isSkipCache(Project project) {
        return project.hasProperty('deploy-dirty')
    }
}