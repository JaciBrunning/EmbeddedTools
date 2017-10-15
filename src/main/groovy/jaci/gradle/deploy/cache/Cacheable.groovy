package jaci.gradle.deploy.cache

trait Cacheable {
    def cache = "md5sum"     // Either a string denoting a CacheMethod, Closure<Boolean> taking DeployContext, or CacheMethod
}