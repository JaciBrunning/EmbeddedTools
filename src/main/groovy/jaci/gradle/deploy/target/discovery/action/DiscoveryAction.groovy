package jaci.gradle.deploy.target.discovery.action

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.discovery.DiscoveryState
import jaci.gradle.deploy.target.location.DeployLocation

import java.util.concurrent.CountDownLatch

@CompileStatic
interface DiscoveryAction {

    DeployContext discover()

    DiscoveryState getState()

    DeployLocation getDeployLocation()

    CountDownLatch getDiscoveryLatch()

}