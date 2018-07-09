package jaci.gradle.deploy.sessions.context

import groovy.transform.CompileStatic
import jaci.gradle.ETLogger
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.discovery.DeployLocation
import jaci.gradle.deploy.sessions.SessionController

// TODO: Session should hold the actual deploy logic
// DryDeployContext and SessionDeployContext should be merged into DefaultDeployContext
// Session should hold _everything_ for the connection.

@CompileStatic
interface DeployContext {
    SessionController getController()

    // Get the deploy logger
    ETLogger getLogger()

    // Get the working directory
    String getWorkingDir()

    DeployLocation getDeployLocation()

    // Run a command (execute)
    String execute(String command)

    // Send a single file
    void put(File source, String dest, CacheMethod cache)

    // Send multiple files, and trigger cache checking only once
    void put(Set<File> files, CacheMethod cache)

    DeployContext subContext(String workingDir)
}
