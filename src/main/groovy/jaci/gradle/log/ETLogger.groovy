package jaci.gradle.log

import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.gradle.internal.logging.text.StyledTextOutput

@CompileStatic
class ETLogger {
    int indent
    String indentStr, name
    boolean silent = false
    Logger internalLogger
    StyledTextOutput colorOut

    ETLogger(String name, StyledTextOutput textOutput, int indent) {
        this.name = name
        this.indent = indent
        this.indentStr = ([' ']*indent).join('')
        this.internalLogger = Logger.getLogger(name)
        this.colorOut = textOutput
    }

    ETLogger(String name, StyledTextOutput textOutput) {
        this(name, textOutput, 0)
    }

    ETLogger push() {
        return new ETLogger(name, colorOut, indent + 2)
    }

    void log(String msg) {
        if (!silent) println(indentStr + msg)

        if (internalLogger.isInfoEnabled())
            internalLogger.info("Log ${silent ? "[silent]" : ""}: ${indentStr + msg}")
    }

    void info(String msg) {
        internalLogger.info(msg)
    }

    void debug(String msg) {
        internalLogger.debug(msg)
    }

    Logger backingLogger() {
        return internalLogger
    }

    void logStyle(String msg, StyledTextOutput.Style style) {
        if (colorOut != null) {
            colorOut.withStyle(style).println(indentStr + msg);
        } else log(msg)
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
