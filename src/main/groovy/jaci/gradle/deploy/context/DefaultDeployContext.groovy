package jaci.gradle.deploy.context

import groovy.transform.CompileStatic
import jaci.gradle.PathUtils
import jaci.gradle.deploy.CommandDeployResult
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.sessions.SessionController
import jaci.gradle.deploy.target.location.DeployLocation
import jaci.gradle.log.ETLogger

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
    CommandDeployResult execute(String command) {
        getController().execute("mkdir -p ${workingDir}")

        logger.log("  -C-> $command @ ${workingDir}")
        def result = session.execute([ "cd ${workingDir}", command].join('\n') )
        if (result != null) {
            if (result.result != null && result.result.length() > 0)
                logger.log("    -[${result.exitCode}]-> ${result.result}")
            else if (result.exitCode != 0)
                logger.log("    -[${result.exitCode}]->")
        }

        return result
    }

    @Override
    void put(Map<String, File> files, CacheMethod cache) {
        session.execute("mkdir -p ${workingDir}")

        Map<String, File> cacheHit = [:], cacheMiss = files

        if (cache != null && !(cache instanceof Boolean && !cache)) {
            if (cache != null && cache.compatible(this)) {
                Set<String> updateRequired = cache.needsUpdate(this, files)
                (files.keySet() - updateRequired).each { String f ->
                    cacheHit[f] = cacheMiss.remove(f)
                }
            }
        }

        if (!cacheMiss.isEmpty()) {
            def entries = cacheMiss.collectEntries { String dst, File src ->
                logger.log("  -F-> ${src} -> ${dst} @ ${workingDir}")
                [(PathUtils.combine(workingDir, dst)): src]
            } as Map<String, File>
            session.put(entries)
        }

        if (cacheHit.size() > 0)
            logger.log("  ${cacheHit.size()} file(s) are up-to-date and were not deployed")
    }

    @Override
    void put(File source, String dest, CacheMethod cache) {
        put([(dest): source], cache)
    }

    @Override
    void put(Set<File> files, CacheMethod cache) {
        put(files.collectEntries { File file ->  [(file.name): file] }, cache)
    }

    @Override
    String friendlyString() {
        return controller.friendlyString()
    }

    @Override
    DeployContext subContext(String workingDir) {
        return new DefaultDeployContext(session, logger.push(), deployLocation, PathUtils.combine(this.workingDir, workingDir))
    }
}
