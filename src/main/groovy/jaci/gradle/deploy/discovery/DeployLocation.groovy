package jaci.gradle.deploy.discovery

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.RemoteTarget

@CompileStatic
interface DeployLocation {
    DiscoveryAction createAction()

    RemoteTarget getTarget()

    String friendlyString()
}