package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.context.DeployContext

@CompileStatic
@InheritConstructors
class DefaultCacheMethod extends AbstractCacheMethod {
    public Closure<Boolean> needsUpdate = { DeployContext ctx, String filename, File localfile -> true }     // true if needs update
    public Closure<Boolean> compatible = { true }

    @Override
    boolean compatible(DeployContext context) {
        return (boolean) ClosureUtils.delegateCall(context, compatible)
    }

    @Override
    Set<String> needsUpdate(DeployContext context, Map<String, File> files) {
        return files.findAll { String name, File file ->
            ClosureUtils.delegateCall(context, needsUpdate, name, file) as boolean
        }.keySet()
    }
}
