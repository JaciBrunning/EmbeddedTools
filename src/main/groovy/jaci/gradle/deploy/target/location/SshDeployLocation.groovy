package jaci.gradle.deploy.target.location

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.target.discovery.action.DiscoveryAction
import jaci.gradle.deploy.target.discovery.action.DryDiscoveryAction
import jaci.gradle.deploy.target.discovery.action.SshDiscoveryAction

@CompileStatic
@InheritConstructors
class SshDeployLocation extends AbstractDeployLocation {

//    List<String> addresses  = []
    String address = null

    boolean ipv6 = false

    String user = null
    String password = ""    // TODO: Change to property. Prompting can be done thru property
//    boolean promptPassword = false

    @Override
    DiscoveryAction createAction() {
        assert address != null
        assert user != null

        if (EmbeddedTools.isDryRun(this.target.project))
            return new DryDiscoveryAction(this)
        return new SshDiscoveryAction(this)
    }

    @Override
    String friendlyString() {
        return "$user @ $address"
    }

    @Override
    String toString() {
        return "SshDeployLocation[$user @ $address]"
    }
}
