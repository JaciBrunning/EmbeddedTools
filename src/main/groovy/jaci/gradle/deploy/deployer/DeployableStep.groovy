package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import jaci.gradle.deploy.DeployContext

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

    void doDeploy(DeployContext ctx) {
        ctx = ctx.subContext(directory)
        precheck.forEach { Closure c -> c.call(ctx) }
        def toRun = true
        if (onlyIf != null) toRun = onlyIf.call(ctx)

        if (toRun) {
            predeploy.forEach { Closure c -> c.call(ctx) }
            deploy(ctx)
            postdeploy.forEach { Closure c -> c.call(ctx) }
        }
    }
    abstract void deploy(DeployContext ctx)

    @Override
    String toString() {
        return "${this.class.simpleName}[${this.name}]".toString()
    }
}
