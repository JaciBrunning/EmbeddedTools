package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named
import org.gradle.api.Project

@CompileStatic
interface Artifact extends Named {
    Project getProject()

    void dependsOn(Object... paths)

    DomainObjectSet<Object> getDependencies()
    DomainObjectSet<Object> getTargets()

    Set<ArtifactDeployTask> getTasks()

    String getDirectory()

    List<Closure> getPredeploy()
    void setPredeploy(List<Closure> actions)

    List<Closure> getPostdeploy()
    void setPostdeploy(List<Closure> actions)

    void runDeploy(DeployContext context)
    void runSkipped(DeployContext context)

    void setOnlyIf(Closure action)

    boolean isEnabled(DeployContext context)

    boolean isDisabled()
    void setDisabled()

    void deploy(DeployContext context)
}