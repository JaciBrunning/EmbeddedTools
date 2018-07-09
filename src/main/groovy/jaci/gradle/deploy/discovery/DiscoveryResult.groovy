package jaci.gradle.deploy.discovery

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jaci.gradle.deploy.sessions.context.DeployContext

@Canonical
@CompileStatic
class DiscoveryResult {
    DeployLocation location
    DiscoveryState state

    // On fail
    DiscoveryFailedException failure

    // On succeed
    DeployContext context
}
