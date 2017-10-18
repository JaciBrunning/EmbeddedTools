package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import jaci.gradle.ClosureUtils
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DeployContext
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.internal.impldep.org.bouncycastle.asn1.x509.Targets

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

    DefaultDomainObjectSet<String> targets = new DefaultDomainObjectSet<String>(String)

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