package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

@CompileStatic
@EqualsAndHashCode(includes = 'name')
class RemoteTarget {
    final String name

    RemoteTarget(String name) {
        this.name = name
    }

    List<String> addresses  = []
    boolean async           = true
    boolean mkdirs          = true
    String directory        = null     // Null = default user home
    String user             = null
    String password         = ""
    boolean promptPassword  = false
    int timeout             = 3
    boolean failOnMissing   = true

    // Calculated Values (from tasks)
    String _active_address  = null

    @Override
    String toString() {
        return "RemoteTarget[${name}]".toString()
    }
}