package jaci.gradle.deploy.target.discovery.action

import groovy.transform.CompileStatic
import jaci.gradle.ETLogger
import jaci.gradle.deploy.context.DefaultDeployContext
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.sessions.DrySessionController
import jaci.gradle.deploy.target.discovery.DiscoveryState
import jaci.gradle.deploy.target.location.DeployLocation
import org.gradle.api.internal.project.ProjectInternal

@CompileStatic
class DryDiscoveryAction implements DiscoveryAction {

    private DeployLocation location
    private ETLogger log

    DryDiscoveryAction(DeployLocation loc) {
        this.location = loc
        this.log = new ETLogger(toString(), ((ProjectInternal)location.target.project).services)
    }

    @Override
    DeployContext discover() {
        DrySessionController controller = new DrySessionController()
        return new DefaultDeployContext(controller, log, location, location.target.directory)
    }

    @Override
    DiscoveryState getState() {
        return DiscoveryState.CONNECTED
    }

    @Override
    DeployLocation getDeployLocation() {
        return location
    }
}
