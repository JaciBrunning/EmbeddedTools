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
class DryDiscoveryAction extends AbstractDiscoveryAction {

    private ETLogger log

    DryDiscoveryAction(DeployLocation loc) {
        super(loc)
        this.log = new ETLogger(toString(), ((ProjectInternal)deployLocation.target.project).services)
    }

    @Override
    DeployContext discover() {
        DrySessionController controller = new DrySessionController()
        def ctx = new DefaultDeployContext(controller, log, deployLocation, deployLocation.target.directory)
        verify(ctx)
        return ctx
    }

    @Override
    DiscoveryState getState() {
        return DiscoveryState.CONNECTED
    }
}
