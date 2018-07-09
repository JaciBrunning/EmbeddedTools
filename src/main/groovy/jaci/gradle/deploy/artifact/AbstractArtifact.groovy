package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.util.Configurable
import org.gradle.util.ConfigureUtil

import java.lang.annotation.Annotation
import java.lang.reflect.Method

@CompileStatic
abstract class AbstractArtifact implements Artifact, Configurable<Artifact> {
    private final String name
    private final Project project

    private DomainObjectSet<Object> dependencies = new DefaultDomainObjectSet<>(Object)
    private DomainObjectSet<Object> targets = new DefaultDomainObjectSet<>(Object)

    private disabled = false

    AbstractArtifact(Project project, String name) {
        this.name = name
        this.project = project
        processAnnotations()
    }

    String getName() {
        return name
    }

    Project getProject() {
        return project
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
    List<Closure> predeploy  = []
    List<Closure> postdeploy = []
    Closure onlyIf           = null

    void setDisabled() {
        disabled = true
    }

    boolean isDisabled() {
        return disabled
    }

    boolean isEnabled(DeployContext ctx) {
        return disabled ? false :
                onlyIf == null ? true :
                        (ClosureUtils.delegateCall(ctx, onlyIf) || ctx.isDryRun())
    }

    AbstractArtifact configure(Closure closure) {
        return ConfigureUtil.configureSelf(closure, this)
    }

    void runDeploy(DeployContext context) {
        getPredeploy().each { Closure c -> ClosureUtils.delegateCall(context, c) }
        deploy(context)
        getPostdeploy().each { Closure c -> ClosureUtils.delegateCall(context, c) }
    }

    void runSkipped(DeployContext context) { }

    private void processAnnotations() {
        for (Class c = this.class; c != null; c = c.getSuperclass()) {
            for (Method method : c.declaredMethods) {
                if (processAnnotation(method, Predeploy.class)) {
                    predeploy << methodWrapper(method)
                }
                if (processAnnotation(method, Postdeploy.class)) {
                    postdeploy << methodWrapper(method)
                }
            }
        }
    }

    private boolean processAnnotation(Method m, Class<? extends Annotation> annotation) {
        if (m.getAnnotation(annotation) == null)
            return false

        final Class<?>[] paramTypes = m.getParameterTypes()
        if (paramTypes.length == 1 && paramTypes[0].equals(DeployContext.class)) {
            return true
        } else {
            throw new GradleException("@${annotation.simpleName} may only be applied to methods taking a DeployContext " +
                    "as the only argument. Problematic method: ${m.name} on ${m.declaringClass.name}")
        }
    }

    private Closure methodWrapper(Method m) {
        return { DeployContext ctx -> m.invoke(this, ctx) }
    }

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