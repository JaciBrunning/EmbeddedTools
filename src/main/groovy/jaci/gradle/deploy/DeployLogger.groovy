package jaci.gradle.deploy

class DeployLogger {
    int indent
    String indentStr

    DeployLogger(int indent) {
        this.indent = indent
        this.indentStr = ([' ']*indent).join('')
    }

    DeployLogger push() {
        return new DeployLogger(indent + 2)
    }

    void log(String msg) {
        println(indentStr + msg)
    }
}
