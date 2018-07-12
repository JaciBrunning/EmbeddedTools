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
