package jaci.gradle.deploy.sessions.context

import groovy.transform.CompileStatic
import jaci.gradle.ETLogger
import jaci.gradle.EmbeddedTools
import jaci.gradle.PathUtils
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.discovery.DeployLocation
import jaci.gradle.deploy.sessions.SessionController

@CompileStatic
class DefaultDeployContext implements DeployContext {

    private SessionController session
    private ETLogger logger
    private DeployLocation deployLocation
    private String workingDir

    DefaultDeployContext(SessionController session, ETLogger logger, DeployLocation deployLocation, String workingDir) {
        this.session = session
        this.logger = logger
        this.deployLocation = deployLocation
        this.workingDir = workingDir
    }

    @Override
    SessionController getController() {
        return session
    }

    @Override
    ETLogger getLogger() {
        return logger
    }

    @Override
    String getWorkingDir() {
        return workingDir
    }

    @Override
    DeployLocation getDeployLocation() {
        return this.deployLocation
    }

    @Override
    String execute(String command) {
        if (deployLocation.target.mkdirs) getController().execute("mkdir -p ${workingDir}")

        logger.log("  -C-> $command @ ${workingDir}")
        def result = session.execute([ "cd ${workingDir}", command].join('\n') )
        if (result != null && result.length() > 0)
            logger.log("    -> $result")

        return result
    }

    private void _put(Map<String, File> files, CacheMethod cache) {
        if (deployLocation.target.mkdirs) session.execute("mkdir -p ${workingDir}")

        if (!EmbeddedTools.isSkipCache(deployLocation.target.project) && cache != null && !(cache instanceof Boolean && !cache)) {
            if (cache != null && cache.compatible(this)) {
                Set<String> updateRequired = cache.needsUpdate(this, files)
                files = files.findAll { String key, File value -> updateRequired.contains(key) }
            }
        }

        files.each { String dst, File src ->
            logger.log("  -F-> ${deployLocation.target.project.rootDir.toURI().relativize(src.toURI()).getPath()} -> ${dst} @ ${workingDir}")
            session.put(src, PathUtils.combine(workingDir, dst))
        }
    }

    @Override
    void put(File source, String dest, CacheMethod cache) {
        _put([(dest): source], cache)
    }

    @Override
    void put(Set<File> files, CacheMethod cache) {
        _put(files.collectEntries { File file ->  [(file.name): file] }, cache)
    }

    @Override
    DeployContext subContext(String workingDir) {
        return new DefaultDeployContext(session, logger.push(), deployLocation, PathUtils.combine(this.workingDir, workingDir))
    }
}
