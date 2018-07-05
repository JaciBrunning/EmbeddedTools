package jaci.gradle.deploy.context

import groovy.transform.CompileStatic
import jaci.gradle.ETLogger
import jaci.gradle.deploy.cache.CacheMethod
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Project

@CompileStatic
interface DeployContext {
    // Get the deploy logger
    ETLogger logger()

    // Get the selected hostname (from discover task)
    String selectedHost()

    // Get the working directory
    String workingDir()

    // Get the target
    RemoteTarget remoteTarget()

    Project getProject()

    // Run a command (execute)
    String execute(String command)

    // Send a single file
    void put(File source, String dest, CacheMethod cache)

    // Send multiple files, and trigger cache checking only once
    void put(Set<File> files, CacheMethod cache)

    DeployContext subContext(String workingDir)
}
