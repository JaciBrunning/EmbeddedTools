package jaci.gradle.deploy.cache

import jaci.gradle.deploy.DeployContext

trait CacheMethod {
    // Returns false if something can't be found (e.g. md5sum). In this case, cache checking is skipped.
    abstract boolean compatible(DeployContext context)
    abstract Set<String> needsUpdate(DeployContext context, Map<String, File> files)
}