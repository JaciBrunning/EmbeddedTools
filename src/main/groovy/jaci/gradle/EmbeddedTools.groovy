package jaci.gradle

import com.jcraft.jsch.JSch
import jaci.gradle.deploy.DeployPlugin
import jaci.gradle.nativedeps.NativeDepsPlugin
import jaci.gradle.toolchains.ToolchainsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.nativeplatform.plugins.NativeComponentPlugin

class EmbeddedTools implements Plugin<Project> {

    ETLogger logger

    @Override
    void apply(Project project) {
        project.extensions.create('em_projectwrapper', ProjectWrapper, project) // TODO: Use ProjectLayout instead

        project.getPluginManager().apply(DeployPlugin)
        logger = new ETLogger(this.class, (project as ProjectInternal).services)

        // Only apply the ToolchainsPlugin and NativeDepsPlugin if we're building a native project.
        project.getPlugins().withType(NativeComponentPlugin).all { NativeComponentPlugin plugin ->
            logger.info("Native Project detected: ${plugin.class.name}".toString())
            project.getPluginManager().apply(ToolchainsPlugin)
            project.getPluginManager().apply(NativeDepsPlugin)
        }
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

    static boolean isDryRun(Project project) {
        return project.hasProperty('deploy-dry')
    }

    static boolean isSkipCache(Project project) {
        return project.hasProperty('deploy-dirty')
    }
}