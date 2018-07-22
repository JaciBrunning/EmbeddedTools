package jaci.gradle.deploy.target.discovery.action

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DefaultDeployContext
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.sessions.DrySessionController
import jaci.gradle.deploy.target.discovery.DiscoveryState
import jaci.gradle.deploy.target.location.DeployLocation
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory

@CompileStatic
class DryDiscoveryAction extends AbstractDiscoveryAction {

    private ETLogger log

    DryDiscoveryAction(DeployLocation loc) {
        super(loc)
        this.log = ETLoggerFactory.INSTANCE.create(toString())
    }

    @Override
    DeployContext discover() {
        DrySessionController controller = new DrySessionController()
        return new DefaultDeployContext(controller, log, deployLocation, deployLocation.target.directory)
    }

    @Override
    DiscoveryState getState() {
        return DiscoveryState.CONNECTED
    }
}
