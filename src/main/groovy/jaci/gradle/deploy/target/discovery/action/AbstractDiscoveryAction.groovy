package jaci.gradle.deploy.target.discovery.action

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.discovery.DiscoveryFailedException
import jaci.gradle.deploy.target.discovery.TargetVerificationException
import jaci.gradle.deploy.target.location.DeployLocation

@CompileStatic
abstract class AbstractDiscoveryAction implements DiscoveryAction {

    private final DeployLocation location
    private DiscoveryFailedException ex = null

    AbstractDiscoveryAction(DeployLocation location) {
        this.location = location
    }

    @Override
    DeployLocation getDeployLocation() {
        return location
    }

    @Override
    DeployContext call() {
        try {
            return discover()
        } catch (Throwable t) {
            def e = new DiscoveryFailedException(this, t)
            if (!(t instanceof InterruptedException))
                ex = e
            throw e
        }
    }

    @Override
    DiscoveryFailedException getException() {
        return ex
    }

    void verify(DeployContext ctx) {
        if (!location.target.verify(ctx))
            throw new TargetVerificationException("Target failed verify (onlyIf) check!")
    }

}
