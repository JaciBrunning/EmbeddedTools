package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.DeployContext
import org.gradle.api.Project

@CompileStatic
@EqualsAndHashCode(includes = 'name')
abstract class DeployableStep {
    final String name

    DeployableStep(String name) {
        this.name = name
    }

    // Closure Args: DeployContext
    List<Closure> precheck        = []  // Called before onlyIF
    Closure<Boolean> onlyIf       = null
    List<Closure> predeploy       = []  // All called after onlyIf
    List<Closure> postdeploy      = []

    int order               = 50
    String directory        = null

    void doDeploy(Project project, DeployContext ctx) {
        ctx = ctx.subContext(directory)
        ctx.logger().log("-> ${toString()}")
        precheck.forEach { Closure c -> ClosureUtils.delegateCall(ctx, c) }

        def toRun = true
        if (onlyIf != null) {
            ctx.logger().log(" -> OnlyIf Check")
            toRun = ClosureUtils.delegateCall(ctx, onlyIf)
            ctx.logger().log(" -> ${toRun ? 'SUCCESS' : 'FAILED'}")
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
