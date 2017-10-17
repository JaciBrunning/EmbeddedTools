package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction

@CompileStatic
class TargetDiscoveryTask extends DefaultTask {
    @Input
    RemoteTarget target

    @TaskAction
    void discoverTarget() {
        // Check if we've already determined the target address
        if (target._active_address != null) {
            println "Target Address Already Determined! (${target._active_address})"
            throw new StopExecutionException()
        }

        // Ask for password if needed
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

        // TODO
        if (target.async) {
            // Try all targets at once. Max time: target.timeout
            def found = []
            println "-> Attempting Target Addresses ${target.addresses.join(', ')}"
//            EmbeddedTools.silenceSsh()
//            try {
//                EmbeddedTools.ssh.run {
//                    target.addresses.each { addr ->
//                        session(host: addr, user: target.user, password: password, timeoutSec: target.timeout, knownHosts: AllowAnyHosts.instance) {
//                            found << addr
//                        }
//                    }
//                }
//            } catch (all) { }
//            EmbeddedTools.unsilenceSsh()

            if (found.size() > 0)
                println "-> Target(s) found at ${found.join(', ')}. Using ${found.last()}"

            if (found.size() > 0)
                target._active_address = found.last()
        } else {
            // Try targets sequentially. Max time: length(target.addresses) * target.timeout
            target.addresses.any { addr ->
                println "-> Attempting Target Address ${addr}"
//                EmbeddedTools.silenceSsh()
//                try {
//                    EmbeddedTools.ssh.run {
//                        session(host: addr, user: target.user, password: password, timeoutSec: target.timeout, knownHosts: AllowAnyHosts.instance) {
//                            println "-> Target found at ${addr}"
//                            target._active_address = addr
//                        }
//                    }
//                } catch (all) { }
//                EmbeddedTools.unsilenceSsh()
                return target._active_address != null
            }
        }

        // Print message about if we didn't find the target. Halt build if configured as such
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
