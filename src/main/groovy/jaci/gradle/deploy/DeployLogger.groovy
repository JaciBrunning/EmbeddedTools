package jaci.gradle.deploy

import groovy.transform.CompileStatic
import org.apache.log4j.Logger

@CompileStatic
class DeployLogger {
    int indent
    String indentStr
    boolean silent = false
    Logger internalLogger

    DeployLogger(int indent) {
        this.indent = indent
        this.indentStr = ([' ']*indent).join('')
        internalLogger = Logger.getLogger(DeployLogger)
    }

    DeployLogger push() {
        return new DeployLogger(indent + 2)
    }

    void log(String msg) {
        if (!silent) println(indentStr + msg)

        if (internalLogger.isInfoEnabled())
            internalLogger.info("Log ${silent ? "[silent]" : ""} ${indentStr + msg}")
    }

    void silent(boolean setSilent) {
        silent = setSilent
    }
}
