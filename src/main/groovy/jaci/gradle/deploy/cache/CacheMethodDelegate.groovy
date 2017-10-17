package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.DeployContext

@CompileStatic
class CacheMethodDelegate implements CacheMethod {
    Closure closureDelegate

    CacheMethodDelegate(Closure closure) {
        this.closureDelegate = closure
    }

    @Override
    boolean compatible(DeployContext context) {
        return true
    }

    @Override
    Set<String> needsUpdate(DeployContext context, Map<String, File> files) {
        return (Set<String>) ClosureUtils.delegateCall(context, closureDelegate, files)
    }
}
