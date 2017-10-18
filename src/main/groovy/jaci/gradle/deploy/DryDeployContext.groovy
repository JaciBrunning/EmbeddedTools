package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.PathUtils
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.cache.CacheMethods
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.transport.SshSessionController
import org.gradle.api.Project

@CompileStatic
class DryDeployContext implements DeployContext {
    String workingDir
    DeployLogger logger
    RemoteTarget target
    Project project

    DryDeployContext(Project project, RemoteTarget target, DeployLogger logger, String workingDir) {
        this.workingDir = workingDir
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

    String _execute(String command) {
        if (target.mkdirs) logger.log("  ~ mkdir -p ${workingDir()}")

        logger.log("  -C-> ${command} @ ${workingDir}")
        logger.log( "    ~ " + [ "cd ${workingDir()}", command ].join(';'))
        return ""
    }

    @Override
    String execute(String command) {
        return _execute(command)
    }

    void put_internal(Map<String, File> files, Object cache) {
        if (target.mkdirs) logger.log("  ~ mkdir -p ${workingDir()}")

        files.each { String dst, File src ->
            logger.log("  -F-> ${project.rootDir.toURI().relativize(src.toURI()).getPath()} -> ${dst} @ ${workingDir}")
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
        return new DryDeployContext(project, target, logger.push(), PathUtils.combine(this.workingDir, workingDir))
    }
}
