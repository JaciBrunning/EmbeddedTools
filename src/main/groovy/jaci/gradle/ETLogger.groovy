package jaci.gradle

import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.internal.service.ServiceRegistry

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

    ETLogger(String name, ServiceRegistry registry, int indent) {
        this(name, registry.get(StyledTextOutputFactory).create(name), indent)
    }

    ETLogger(String name, ServiceRegistry registry) {
        this(name, registry, 0)
    }

    ETLogger(Class clazz, ServiceRegistry registry) {
        this(clazz.name, registry)
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
