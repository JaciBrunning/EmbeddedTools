package jaci.gradle.deploy.target.discovery.action

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.location.DeployLocation

import java.util.concurrent.CountDownLatch

@CompileStatic
abstract class AbstractDiscoveryAction implements DiscoveryAction {

    private final DeployLocation location
    private final CountDownLatch theLatch = new CountDownLatch(1)

    AbstractDiscoveryAction(DeployLocation location) {
        this.location = location
    }

    @Override
    DeployLocation getDeployLocation() {
        return location
    }

    @Override
    CountDownLatch getDiscoveryLatch() {
        return theLatch
    }
}
