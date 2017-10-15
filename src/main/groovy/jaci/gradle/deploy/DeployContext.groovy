package jaci.gradle.deploy

interface DeployContext {
    String workingDir()

    String runCommand(String command)
    boolean sendFile(File source, String dest, cache)

    DeployContext subContext(String workingDir)
}
