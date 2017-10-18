package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.gradle.api.Named

@CompileStatic
@EqualsAndHashCode(includes = 'name')
class RemoteTarget implements Named {
    final String name

    RemoteTarget(String name) {
        this.name = name
    }

    List<String> addresses  = []
    boolean mkdirs          = true
    String directory        = null     // Null = default user home
    String user             = null
    String password         = ""
    boolean promptPassword  = false
    int timeout             = 3
    boolean failOnMissing   = true

    @Override
    String toString() {
        return "RemoteTarget[${name}]".toString()
    }
}