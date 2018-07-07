package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@CompileStatic
interface Artifact extends Named {
    void dependsOn(Object... paths)
    void after(Object... artifacts)

    DomainObjectSet<Object> getDependencies()
    DomainObjectSet<Object> getTargets()

    String getDirectory()

    List<Closure> getPredeploy()
    void setPredeploy(List<Closure> actions)

    List<Closure> getPostdeploy()
    void setPostdeploy(List<Closure> actions)

    void setOnlyIf(Closure action)

    boolean isEnabled(DeployContext context)

    boolean isDisabled()
    void setDisabled()

    void deploy(DeployContext context)
}