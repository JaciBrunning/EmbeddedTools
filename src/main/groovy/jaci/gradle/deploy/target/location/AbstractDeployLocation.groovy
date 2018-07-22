package jaci.gradle.deploy.target.location

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.location.DeployLocation

@CompileStatic
abstract class AbstractDeployLocation implements DeployLocation {

    final RemoteTarget target

    AbstractDeployLocation(RemoteTarget target) {
        this.target = target
    }

    @Override
    RemoteTarget getTarget() {
        return this.target
    }
}
