package jaci.gradle.deploy.tasks

import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.hidetake.groovy.ssh.connection.AllowAnyHosts

class TargetDiscoveryTask extends DefaultTask {
    @Input
    RemoteTarget target

    @TaskAction
    void discoverTarget() {
        if (target._active_address != null) {
            println "Target Address Already Determined! (${target._active_address})"
            throw new StopExecutionException()
        }

        def password = target.password ?: ""
        if (target.promptPassword) {
            def tpassword = EmbeddedTools.promptPassword(target.user)
            if (tpassword != null) password = tpassword
        }

        // We only have to prompt once
        target.password = password
        target.promptPassword = false

        // TODO Better Logging
        println("Discovering Target ${target.name}")
        // Assertions
        assert target.user != null
        assert target.timeout > 0

        if (target.async) {
            def found = []
            EmbeddedTools.silenceSsh()
            println "-> Attempting Target Addresses ${target.addresses.join(', ')}"
            try {
                EmbeddedTools.ssh.run {
                    target.addresses.each { addr ->
                        session(host: addr, user: target.user, password: password, timeoutSec: target.timeout, knownHosts: AllowAnyHosts.instance) {
                            found << addr
                        }
                    }
                }
            } catch (all) { }
            println "-> Target(s) found at ${found.join(', ')}"
            EmbeddedTools.unsilenceSsh()

            if (found.size() > 0) target._active_address = found.last()
        } else {
            // do sequential
        }

        if (target._active_address == null) {
            if (target.failOnMissing)
                throw new TargetNotFoundException("Target ${target.name} could not be located! Failing as ${target.name}.failOnMissing is true.")
            else
                println "Target ${target.name} could not be located! Skipping target as ${target.name}.failOnMissing is false."
        }
    }

    class TargetNotFoundException extends RuntimeException {
        TargetNotFoundException(String msg) {
            super(msg)
        }
    }
}
