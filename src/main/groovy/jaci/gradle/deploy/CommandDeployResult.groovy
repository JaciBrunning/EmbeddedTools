package jaci.gradle.deploy

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class CommandDeployResult {
    String command
    String result
    int exitCode
}
