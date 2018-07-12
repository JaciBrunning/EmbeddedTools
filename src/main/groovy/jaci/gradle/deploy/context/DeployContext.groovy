package jaci.gradle.deploy.context

import groovy.transform.CompileStatic
import jaci.gradle.ETLogger
import jaci.gradle.deploy.CommandDeployResult
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.sessions.SessionController
import jaci.gradle.deploy.target.location.DeployLocation

@CompileStatic
interface DeployContext {
    SessionController getController()

    // Get the deploy logger
    ETLogger getLogger()

    // Get the working directory
    String getWorkingDir()

    DeployLocation getDeployLocation()

    // Run a command (execute)
    CommandDeployResult execute(String command)

    // Send a batch of files
    void put(Map<String, File> files, CacheMethod cache)

    // Send a single file
    void put(File source, String dest, CacheMethod cache)

    // Send multiple files, and trigger cache checking only once
    void put(Set<File> files, CacheMethod cache)

    DeployContext subContext(String workingDir)
}
