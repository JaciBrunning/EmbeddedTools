package jaci.gradle.deploy.cache

trait Cacheable {
    // Either a boolean false, string denoting a CacheMethod, Closure<Boolean> taking DeployContext, or CacheMethod
    def cache = "md5sum"
}