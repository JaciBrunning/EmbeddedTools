package jaci.gradle.deploy

import groovy.transform.CompileStatic
import jaci.gradle.ETLogger
import jaci.gradle.PathUtils
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Project

@CompileStatic
class DryDeployContext implements DeployContext {
    String workingDir
    ETLogger logger
    String targetAddr
    RemoteTarget target
    Project project

    DryDeployContext(Project project, RemoteTarget target, String targetAddr, ETLogger logger, String workingDir) {
        this.workingDir = workingDir
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

    @Override
    Project project() {
        return project
    }

    String _execute(String command) {
        if (target.mkdirs) logger.log("  ~ mkdir -p ${workingDir()}")

        logger.log("  -C-> ${command} @ ${workingDir}")
        logger.log( "    ~ " + [ "cd ${workingDir()}", command ].join('; '))
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
        return new DryDeployContext(project, target, targetAddr, logger.push(), PathUtils.combine(this.workingDir, workingDir))
    }
}
