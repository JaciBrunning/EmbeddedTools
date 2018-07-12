package jaci.gradle.deploy.target.discovery.action

import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.discovery.DiscoveryState
import jaci.gradle.deploy.target.location.DeployLocation

interface DiscoveryAction {

    // Discovery should be setup for one property per address, each of which will be put into the
    // threadpool

    DeployContext discover()

    DiscoveryState getState()

    DeployLocation getDeployLocation()

}