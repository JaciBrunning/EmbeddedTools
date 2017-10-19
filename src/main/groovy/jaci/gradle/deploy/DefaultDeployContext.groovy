package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.PathUtils
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.cache.CacheMethods
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.transport.SshSessionController
import org.gradle.api.Project

@CompileStatic
class DefaultDeployContext implements DeployContext {
    String workingDir
    SshSessionController session
    DeployLogger logger
    RemoteTarget target
    Project project

    DefaultDeployContext(Project project, RemoteTarget target, DeployLogger logger, SshSessionController session, String workingDir) {
        this.workingDir = workingDir
        this.session = session
        this.logger = logger
        this.target = target
        this.project = project
    }

    @Override
    DeployLogger logger() {
        return logger
    }

    @Override
    String workingDir() {
        return workingDir
    }

    @Override
    Project project() {
        return project
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

    void put_internal(Map<String, File> files, Object cache) {
        if (target.mkdirs) session.execute("mkdir -p ${workingDir()}")

        if (!EmbeddedTools.isSkipCache(project) && cache != null && !(cache instanceof Boolean && cache == false)) {
            CacheMethod cacheMethod = CacheMethods.getMethod(cache)
            if (cacheMethod != null && cacheMethod.compatible(this)) {
                Set<String> updateRequired = cacheMethod.needsUpdate(this, files)
                files = files.findAll { String key, File value -> updateRequired.contains(key) }
            }
        }

        files.each { String dst, File src ->
            logger.log("  -F-> ${project.rootDir.toURI().relativize(src.toURI()).getPath()} -> ${dst} @ ${workingDir()}")
            session.put(src, PathUtils.combine(workingDir(), dst))
        }
    }

    @Override
    void put(File source, String dest, Object cache) {
        put_internal([(dest): source], cache)
    }

    @Override
    void put(Set<File> files, Object cache) {
        put_internal(files.collectEntries { File file ->  [(file.name): file] }, cache)
    }

    @Override
    DeployContext subContext(String workingDir) {
        return new DefaultDeployContext(project, target, logger.push(), session, PathUtils.combine(this.workingDir, workingDir))
    }
}
