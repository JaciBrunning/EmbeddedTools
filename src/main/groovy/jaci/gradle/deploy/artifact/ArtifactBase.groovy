package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import jaci.gradle.ClosureUtils
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.tasks.ArtifactDeployTask
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.DefaultDomainObjectSet

@CompileStatic
@EqualsAndHashCode(includes = 'name')
abstract class ArtifactBase implements Named {
    final String name

    ArtifactBase(String name) {
        this.name = name
    }

    // Closure Args: DeployContext
    List<Closure> precheck        = []  // Called before onlyIf
    Closure<Boolean> onlyIf       = null
    List<Closure> predeploy       = []  // Called after onlyIf but before artifact action
    List<Closure> postdeploy      = []  // Called after artifact action

    String directory        = null

    DefaultDomainObjectSet<String>  targets         = new DefaultDomainObjectSet<>(String)
    DefaultDomainObjectSet<Object>  dependencies    = new DefaultDomainObjectSet<>(Object)
    Set<Task> taskDependencies

    void dependsOn(Object task) {
        dependencies << task
    }

    void after(Object... artifacts) {
        artifacts.each { Object artifact ->
            if (artifact instanceof String) {
                dependencies << { Project project ->
                    project.tasks.withType(ArtifactDeployTask).matching { ArtifactDeployTask t -> t.artifact.name == artifact }
                } as Action<? extends Project>
            } else if (artifact instanceof ArtifactBase) {
                dependencies << { Project project ->
                    project.tasks.withType(ArtifactDeployTask).matching { ArtifactDeployTask t -> t == artifact }
                } as Action<? extends Project>
            }
        }
    }

    // Internal
    void doDeploy(Project project, DeployContext ctx) {
        ctx = ctx.subContext(directory)
        ctx.logger().log("-> ${toString()}")
        precheck.forEach { Closure c -> ClosureUtils.delegateCall(ctx, c) }

        def toRun = true
        if (onlyIf != null) {
            ctx.logger().log(" -> OnlyIf Check")
            toRun = ClosureUtils.delegateCall(ctx, onlyIf) || EmbeddedTools.isDryRun(project)
            ctx.logger().log(" -> ${EmbeddedTools.isDryRun(project) ? 'DRY' : toRun ? 'SUCCESS' : 'FAILED'}")
        }

        if (toRun) {
            predeploy.forEach { Closure c -> ClosureUtils.delegateCall(ctx, c) }
            deploy(project, ctx)
            postdeploy.forEach { Closure c -> ClosureUtils.delegateCall(ctx, c) }
        }
        ctx.logger().log("")
    }
    abstract void deploy(Project project, DeployContext ctx)

    @Override
    String toString() {
        return "${this.class.simpleName}[${this.name}]".toString()
    }
}