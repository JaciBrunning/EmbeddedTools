package jaci.gradle.deploy.context

import groovy.transform.CompileStatic
import jaci.gradle.ETLogger
import jaci.gradle.EmbeddedTools
import jaci.gradle.PathUtils
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.transport.SshSessionController
import org.gradle.api.Project

@CompileStatic
class SshDeployContext implements DeployContext {
    String workingDir
    SshSessionController session
    ETLogger logger
    String targetAddr
    RemoteTarget target
    Project project

    SshDeployContext(Project project, RemoteTarget target, String targetAddr, ETLogger logger, SshSessionController session, String workingDir) {
        this.workingDir = workingDir
        this.session = session
        this.logger = logger
        this.targetAddr = targetAddr
        this.target = target
        this.project = project
    }

    @Override
    ETLogger logger() {
        return logger
    }

    @Override
    String selectedHost() {
        return targetAddr
    }

    @Override
    String workingDir() {
        return workingDir
    }

    @Override
    RemoteTarget remoteTarget() {
        return target
    }

    String _execute(String command) {
        if (target.mkdirs) session.execute("mkdir -p ${workingDir()}")

        logger.log("  -C-> ${command} @ ${workingDir}")
        def result = session.execute([ "cd ${workingDir()}", command ].join('\n'))
        if (result != null && result.length() > 0)
            logger.log("   -> ${result}")
        return result
    }

    @Override
    String execute(String command) {
        return _execute(command)
    }

    void put_internal(Map<String, File> files, CacheMethod cache) {
        if (target.mkdirs) session.execute("mkdir -p ${workingDir()}")

        if (!EmbeddedTools.isSkipCache(project) && cache != null && !(cache instanceof Boolean && !cache)) {
            if (cache != null && cache.compatible(this)) {
                Set<String> updateRequired = cache.needsUpdate(this, files)
                files = files.findAll { String key, File value -> updateRequired.contains(key) }
            }
        }

        files.each { String dst, File src ->
            logger.log("  -F-> ${project.rootDir.toURI().relativize(src.toURI()).getPath()} -> ${dst} @ ${workingDir()}")
            session.put(src, PathUtils.combine(workingDir(), dst))
        }
    }

    @Override
    void put(File source, String dest, CacheMethod cache) {
        put_internal([(dest): source], cache)
    }

    @Override
    void put(Set<File> files, CacheMethod cache) {
        put_internal(files.collectEntries { File file ->  [(file.name): file] }, cache)
    }

    @Override
    DeployContext subContext(String workingDir) {
        return new SshDeployContext(project, target, targetAddr, logger.push(), session, PathUtils.combine(this.workingDir, workingDir))
    }
}
