package jaci.gradle.deploy.context

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.DefaultNamedDomainObjectSet
import org.gradle.internal.reflect.DirectInstantiator

// TODO: This shouldn't function with DeployContext, but with another type
// TODO: that constructs DeployContext and the necessary discovery technique(s),
// TODO: as well as target properties.
@CompileStatic
class DeployContextExtension extends DefaultNamedDomainObjectSet<DeployContext> {

    Project project

    DeployContextExtension(Project project) {
        super(DeployContext.class, DirectInstantiator.INSTANCE)
        this.project = project
    }
}
