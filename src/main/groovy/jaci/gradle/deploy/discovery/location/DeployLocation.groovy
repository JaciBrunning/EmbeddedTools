package jaci.gradle.deploy.discovery.location

import groovy.transform.CompileStatic
import jaci.gradle.deploy.discovery.action.DiscoveryAction
import jaci.gradle.deploy.target.RemoteTarget

@CompileStatic
interface DeployLocation {
    DiscoveryAction createAction()

    RemoteTarget getTarget()

    String friendlyString()
}