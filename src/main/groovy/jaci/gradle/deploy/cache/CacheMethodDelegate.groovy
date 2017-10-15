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
    boolean needsUpdate(DeployContext context, File localFile, String file) {
        return ClosureUtils.delegateCall(context, closureDelegate, localFile, file)
    }
}
