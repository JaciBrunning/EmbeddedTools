package jaci.gradle.deploy.sessions

import groovy.transform.CompileStatic
import jaci.gradle.deploy.CommandDeployResult

@CompileStatic
interface SessionController extends Closeable {

    void open()

    CommandDeployResult execute(String command)

    void put(Map<String, File> files)

    String friendlyString()

}