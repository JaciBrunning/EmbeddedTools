package jaci.gradle.deploy.discovery

import groovy.transform.CompileStatic

@CompileStatic
class DiscoveryFailedException extends RuntimeException {
    DiscoveryAction action

    DiscoveryFailedException(DiscoveryAction action, Throwable cause) {
        super(cause)
        this.action = action
    }
}
