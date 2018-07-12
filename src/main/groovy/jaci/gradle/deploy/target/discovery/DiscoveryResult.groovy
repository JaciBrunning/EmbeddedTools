package jaci.gradle.deploy.target.discovery

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.location.DeployLocation

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
