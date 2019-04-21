package jaci.gradle.deploy.target.location

import groovy.transform.CompileStatic
import jaci.gradle.deploy.target.RemoteTarget
import org.gradle.api.Action
import org.gradle.api.internal.DefaultDomainObjectSet

@CompileStatic
class DeployLocationSet extends DefaultDomainObjectSet<DeployLocation> {

    final RemoteTarget target

    DeployLocationSet(RemoteTarget target) {
        super(DeployLocation)
        this.target = target
    }

    DeployLocation location(Class<? extends DeployLocation> type, final Action<? extends DeployLocation> config) {
        DeployLocation location = target.project.objects.newInstance(type, target)

        if (target.isDry())
            location = new DryDeployLocation(location)
        else
            config.execute(location)

        this << location
        return location
    }

    DeployLocation ssh(final Action<? extends DeployLocation> config) {
        return location(SshDeployLocation, config)
    }
}
