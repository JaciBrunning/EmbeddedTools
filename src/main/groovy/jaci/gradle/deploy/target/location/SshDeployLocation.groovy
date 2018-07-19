package jaci.gradle.deploy.target.location

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.deploy.target.discovery.action.DiscoveryAction
import jaci.gradle.deploy.target.discovery.action.SshDiscoveryAction

@CompileStatic
@InheritConstructors
class SshDeployLocation extends AbstractDeployLocation {

    String address = null

    boolean ipv6 = false

    String user = null
    String password = ""

    @Override
    DiscoveryAction createAction() {
        if (address == null || user == null)
            throw new IllegalArgumentException("Address and User must not be null for SshDeployLocation")

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
