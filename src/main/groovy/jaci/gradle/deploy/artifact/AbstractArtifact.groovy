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

    AbstractArtifact(String name, Project project) {
        this.name = name
        this.project = project
        processAnnotations()
    }

    Project getProject() {
        return project
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

    Set<ArtifactDeployTask> getTasks() {
        return project.tasks.withType(ArtifactDeployTask).findAll { ArtifactDeployTask t ->
            t.artifact == this
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
                        (ClosureUtils.delegateCall(ctx, onlyIf) || ctx.deployLocation.target.isDry())
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

    @Override
    String toString() {
        return "${this.class.simpleName}[${this.name}]".toString()
    }
}