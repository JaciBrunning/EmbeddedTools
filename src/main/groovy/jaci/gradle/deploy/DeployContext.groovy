package jaci.gradle.deploy

import groovy.transform.CompileStatic

@CompileStatic
interface DeployContext {
    // Get the working directory
    String workingDir()

    // Run a command (execute)
    String execute(String command)

    // Send a single file
    boolean put(File source, String dest, cache)

    // Gives a closure access to the ssh session
    void withSession(Closure closure)

    DeployContext subContext(String workingDir)
}
