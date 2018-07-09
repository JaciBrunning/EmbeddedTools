package jaci.gradle.deploy.discovery

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.RemoteTarget

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
