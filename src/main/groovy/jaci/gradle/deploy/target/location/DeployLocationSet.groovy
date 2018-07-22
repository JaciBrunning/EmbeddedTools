package jaci.gradle.deploy.target.location

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.util.ConfigureUtil

@CompileStatic
class DeployLocationSet extends DefaultDomainObjectSet<DeployLocation> {

    final RemoteTarget target

    DeployLocationSet(RemoteTarget target) {
        super(DeployLocation)
        this.target = target
    }

    DeployLocation location(Class<? extends DeployLocation> type, final Closure config) {
        def location = type.newInstance(target)

        if (target.isDry())
            location = new DryDeployLocation(location)
        else
            ConfigureUtil.configure(config, location);

        this << location
        return location
    }

    DeployLocation ssh(final Closure config) {
        return location(SshDeployLocation, config)
    }
}
