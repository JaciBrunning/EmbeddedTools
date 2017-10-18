package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.DefaultNamedDomainObjectSet
import org.gradle.internal.reflect.DirectInstantiator

// DefaultNamedDomainObjectSet applies the withType, matching, all and other methods
// that are incredibly useful
@CompileStatic
class TargetsExtension extends DefaultNamedDomainObjectSet<RemoteTarget> {
    Project project
    TargetsExtension(Project project) {
        super(RemoteTarget, DirectInstantiator.INSTANCE)
        this.project = project
    }

    def target(String name, Class<? extends RemoteTarget> type, final Closure config) {
        def target = type.newInstance(name)
        project.configure(target, config)
        this << (target)
    }

    def target(String name, final Closure config) {
        target(name, RemoteTarget, config)
    }
}
