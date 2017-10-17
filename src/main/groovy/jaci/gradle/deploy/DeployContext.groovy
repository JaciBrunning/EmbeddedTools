package jaci.gradle.deploy

import groovy.transform.CompileStatic

@CompileStatic
interface DeployContext {
    // Get the deploy logger
    DeployLogger logger()

    // Get the working directory
    String workingDir()

    // Run a command (execute)
    String execute(String command)

    // Run a command (execute), ignoring error
    String executeMaybe(String command)

    // Send a single file
    void put(File source, String dest, cache)

    // Send multiple files, and trigger cache checking only once
    void put(Set<File> files, cache)

    DeployContext subContext(String workingDir)
}
