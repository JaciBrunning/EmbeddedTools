package jaci.gradle

import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.internal.service.ServiceRegistry

@CompileStatic
class IndentedLogger {
    int indent
    String indentStr
    boolean silent = false
    Logger internalLogger
    ServiceRegistry registry
    StyledTextOutput colorOut

    IndentedLogger(ServiceRegistry registry, int indent) {
        this.indent = indent
        this.indentStr = ([' ']*indent).join('')
        internalLogger = Logger.getLogger(IndentedLogger)
        colorOut = registry.get(StyledTextOutputFactory).create("IndentedLogger")
        this.registry = registry
    }

    IndentedLogger push() {
        return new IndentedLogger(registry, indent + 2)
    }

    void log(String msg) {
        if (!silent) println(indentStr + msg)

        if (internalLogger.isInfoEnabled())
            internalLogger.info("Log ${silent ? "[silent]" : ""} ${indentStr + msg}")
    }

    void logStyle(String msg, StyledTextOutput.Style style) {
        colorOut.withStyle(style).println(indentStr + msg);
    }

    void logError(String msg) {
        logStyle(msg, StyledTextOutput.Style.Failure)
    }

    void logErrorHead(String msg) {
        logStyle(msg, StyledTextOutput.Style.FailureHeader)
    }

    void silent(boolean setSilent) {
        silent = setSilent
    }
}
