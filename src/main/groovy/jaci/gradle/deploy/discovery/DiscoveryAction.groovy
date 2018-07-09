package jaci.gradle.deploy.discovery

import jaci.gradle.deploy.sessions.context.DeployContext

interface DiscoveryAction {

    // Discovery should be setup for one property per address, each of which will be put into the
    // threadpool

    DeployContext discover()

    DiscoveryState getState()

    DeployLocation getDeployLocation()

}