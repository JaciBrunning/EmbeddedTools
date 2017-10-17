package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.PathUtils
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.cache.CacheMethods
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Project
import org.hidetake.groovy.ssh.session.SessionHandler

@CompileStatic
class DefaultDeployContext implements DeployContext {
    String workingDir
    SessionHandler handler
    DeployLogger logger
    RemoteTarget target
    Project project

    DefaultDeployContext(Project project, RemoteTarget target, DeployLogger logger, SessionHandler sessionHandler, String workingDir) {
        this.workingDir = workingDir
        this.handler = sessionHandler
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

    String _execute(String command, HashMap map = [:]) {
        if (target.mkdirs) handler.execute("mkdir -p ${workingDir()}")

        logger.log("  -C-> ${command} @ ${workingDir}")
        def result = handler.execute(map, [ "cd ${workingDir()}", command ].join('\n'))
        if (result != null && result.length() > 0)
            logger.log("   -> ${result}")
        return result
    }

    @Override
    String execute(String command) {
        return _execute(command)
    }

    @Override
    String executeMaybe(String command) {
        return _execute(command, [ignoreError: true])
    }

    void put_internal(Map<String, File> files, Object cache) {
        if (target.mkdirs) handler.execute("mkdir -p ${workingDir()}")

        if (!project.hasProperty('skip-cache') && cache != null && !(cache instanceof Boolean && cache == false)) {
            CacheMethod cacheMethod = CacheMethods.getMethod(cache)
            if (cacheMethod != null && cacheMethod.compatible(this)) {
                Set<String> updateRequired = cacheMethod.needsUpdate(this, files)
                files = files.findAll { String key, File value -> updateRequired.contains(key) }
            }
        }

        files.each { String dst, File src ->
            logger.log("  -F-> ${project.rootDir.toURI().relativize(src.toURI()).getPath()} -> ${dst} @ ${workingDir}")
            handler.put(from: src, into: PathUtils.combine(workingDir(), dst))
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
    void withSession(Closure closure) {
        ClosureUtils.delegateCall(handler, closure, this)
    }

    @Override
    DeployContext subContext(String workingDir) {
        return new DefaultDeployContext(project, target, logger.push(), handler, PathUtils.combine(this.workingDir, workingDir))
    }
}
