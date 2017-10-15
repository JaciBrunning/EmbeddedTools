package jaci.gradle.deploy.cache

import jaci.gradle.deploy.DeployContext

interface CacheMethod {
    // Returns false if something can't be found (e.g. md5sum). In this case, cache checking is skipped.
    boolean compatible(DeployContext context)
    boolean needsUpdate(DeployContext context, String file)
}