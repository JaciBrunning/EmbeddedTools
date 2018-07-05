package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named

@CompileStatic
interface Artifact extends Named {
    void dependsOn(Object... paths)
    void after(Object... artifacts)

    DomainObjectSet<Object> getDependencies()
    DomainObjectSet<Object> getTargets()

    String getDirectory()

    void deploy(DeployContext context)
}