package jaci.gradle.deploy.sessions.ssh

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.deploy.discovery.AbstractDeployLocation
import jaci.gradle.deploy.discovery.DiscoveryAction

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
