package jaci.gradle.deploy.target.location

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.discovery.action.DiscoveryAction
import jaci.gradle.deploy.target.discovery.action.DryDiscoveryAction

@CompileStatic
class DryDeployLocation extends AbstractDeployLocation {

    private DeployLocation inner

    DryDeployLocation(DeployLocation inner) {
        super(inner.target)
        this.inner = inner
    }

    @Override
    DiscoveryAction createAction() {
        return new DryDiscoveryAction(inner)
    }

    @Override
    String friendlyString() {
        return "DryRun DeployLocation (wrapping ${inner.toString()})"
    }
}
