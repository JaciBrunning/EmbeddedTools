package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext
import org.gradle.api.Named

@CompileStatic
interface CacheMethod extends Named {
    // Returns false if something can't be found (e.g. md5sum). In this case, cache checking is skipped.
    boolean compatible(DeployContext context)
    Set<String> needsUpdate(DeployContext context, Map<String, File> files)
}