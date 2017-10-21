package jaci.gradle.deploy

import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
interface DeployContext {
    // Get the deploy logger
    DeployLogger logger()

    // Get the selected hostname (from discover task)
    String selectedHost()

    // Get the working directory
    String workingDir()

    // Get the project
    Project project()

    // Run a command (execute)
    String execute(String command)

    // Send a single file
    void put(File source, String dest, cache)

    // Send multiple files, and trigger cache checking only once
    void put(Set<File> files, cache)

    DeployContext subContext(String workingDir)
}
