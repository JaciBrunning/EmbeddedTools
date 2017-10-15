package jaci.gradle.deploy

import jaci.gradle.PathUtils

class DebugDeployContext implements DeployContext {
    int indent
    String workingDir

    public DebugDeployContext(String workDir, int indent) {
        this.indent = indent
        this.workingDir = workDir
    }

    @Override
    String workingDir() {
        return workingDir
    }

    @Override
    String execute(String command) {
        println((['']*indent).join(' ') + "-C-> ${command} @ ${workingDir}")
        return "<return>"
    }

    @Override
    boolean put(File source, String dest, Object cache) {
        println((['']*indent).join(' ') + "-F-> ${source.absolutePath} --> ${dest} @ ${workingDir}")
        return true
    }

    @Override
    void withSession(Closure closure) { }

    @Override
    DeployContext subContext(String workingDir) {
        return new DebugDeployContext(PathUtils.combine(this.workingDir, workingDir), indent+2)
    }
}
