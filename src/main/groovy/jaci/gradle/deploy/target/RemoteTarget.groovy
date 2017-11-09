package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.gradle.api.Named

import java.util.concurrent.CountDownLatch

@CompileStatic
@EqualsAndHashCode(includes = 'name')
class RemoteTarget implements Named {
    final String name

    RemoteTarget(String name) {
        this.name = name
    }

    List<String> addresses  = []
    boolean mkdirs          = true
    boolean ipv6            = false    // Enable IPv6 resolution? (experimental)
    boolean discoverInstant = true
    String directory        = null     // Null = default user home
    String user             = null
    String password         = ""
    boolean promptPassword  = false
    int timeout             = 3
    boolean failOnMissing   = true

    // Internal
    CountDownLatch latch = new CountDownLatch(1)

    @Override
    String toString() {
        return "RemoteTarget[${name}]".toString()
    }
}