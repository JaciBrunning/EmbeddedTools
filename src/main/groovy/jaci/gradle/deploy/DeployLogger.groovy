package jaci.gradle.deploy

class DeployLogger {
    int indent
    String indentStr
    boolean silent = false

    DeployLogger(int indent) {
        this.indent = indent
        this.indentStr = ([' ']*indent).join('')
    }

    DeployLogger push() {
        return new DeployLogger(indent + 2)
    }

    void log(String msg) {
        if (!silent) println(indentStr + msg)
    }

    void silent(boolean setSilent) {
        silent = setSilent
    }
}
