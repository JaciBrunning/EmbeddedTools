package jaci.gradle.deploy.target.location

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.discovery.action.DiscoveryAction

@CompileStatic
interface DeployLocation {
    DiscoveryAction createAction()

    RemoteTarget getTarget()

    String friendlyString()
}