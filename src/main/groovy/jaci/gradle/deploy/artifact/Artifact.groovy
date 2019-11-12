package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.Action
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.tasks.TaskCollection
import java.util.function.Predicate

@CompileStatic
interface Artifact extends Named {
    Project getProject()

    void dependsOn(Object... paths)

    List<Action<Artifact>> getPreWorkerThread()

    DomainObjectSet<Object> getDependencies()
    DomainObjectSet<Object> getTargets()

    TaskCollection<ArtifactDeployTask> getTasks()

    String getDirectory()

    List<Action<DeployContext>> getPredeploy()

    List<Action<DeployContext>> getPostdeploy()

    void setOnlyIf(Predicate<DeployContext> action)

    boolean isEnabled(DeployContext context)

    boolean isDisabled()
    void setDisabled()

    void deploy(DeployContext context)

    boolean isExplicit()
    void setExplicit(boolean explicit)
}
