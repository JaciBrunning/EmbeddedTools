package jaci.gradle.deploy.sessions

import groovy.transform.CompileStatic

@CompileStatic
interface SessionController extends Closeable {

    void open()

    String execute(String command)

    void put(List<File> sources, List<String> destinations)
    void put(File source, String dest)

    String friendlyString()

}