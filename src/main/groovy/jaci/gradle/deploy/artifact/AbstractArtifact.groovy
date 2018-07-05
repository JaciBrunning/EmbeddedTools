package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.DomainObjectSet
import org.gradle.api.internal.DefaultDomainObjectSet

@CompileStatic
abstract class AbstractArtifact implements Artifact {
    private final String name

    private DomainObjectSet<Object> dependencies = new DefaultDomainObjectSet<>(Object)
    private DomainObjectSet<Object> targets = new DefaultDomainObjectSet<>(Object)

    AbstractArtifact(String name) {
        this.name = name
    }

    String getName() {
        return name
    }

    DomainObjectSet<Object> getDependencies() {
        return dependencies
    }

    DomainObjectSet<Object> getTargets() {
        return targets
    }

    void dependsOn(Object... paths) {
        for (Object val : paths)
            dependencies.add(val)
    }

    void after(Object... artifacts) {
        for (Object artifact : artifacts) {
            // TODO: Use an artifact resolver here
//            dependsOn({ Task t -> t.project. } as Closure)
        }
    }

    // Groovy generates get/set
    String directory = null

//    void after(Object... artifacts) {
//        // TODO: Make this use resolver
//        artifacts.each { Object artifact ->
//            if (artifact instanceof String) {
//                dependencies << { Project project ->
//                    project.tasks.withType(ArtifactDeployTask).matching { ArtifactDeployTask t -> t.artifact.name == artifact }
//                } as Action<? extends Project>
//            } else if (artifact instanceof Artifact) {
//                dependencies << { Project project ->
//                    project.tasks.withType(ArtifactDeployTask).matching { ArtifactDeployTask t -> t == artifact }
//                } as Action<? extends Project>
//            }
//        }
//    }

    // Internal
//    void doDeploy(Project project, DeployContext ctx) {
//        ctx = ctx.subContext(directory)
//        ctx.logger().log("-> ${toString()}")
//        precheck.forEach { Closure c -> ClosureUtils.delegateCall(ctx, c) }
//
//        def toRun = true
//        if (onlyIf != null) {
//            ctx.logger().log(" -> OnlyIf Check")
//            toRun = ClosureUtils.delegateCall(ctx, onlyIf) || EmbeddedTools.isDryRun(project)
//            ctx.logger().log(" -> ${EmbeddedTools.isDryRun(project) ? 'DRY' : toRun ? 'OnlyIf triggered' : 'OnlyIf not triggered'}")
//        }
//
//        if (toRun) {
//            predeploy.each { Closure c -> ClosureUtils.delegateCall(ctx, c) }
//            deploy(project, ctx)
//            postdeploy.each { Closure c -> ClosureUtils.delegateCall(ctx, c) }
//        }
//        ctx.logger().log("")
//    }

    @Override
    String toString() {
        return "${this.class.simpleName}[${this.name}]".toString()
    }
}