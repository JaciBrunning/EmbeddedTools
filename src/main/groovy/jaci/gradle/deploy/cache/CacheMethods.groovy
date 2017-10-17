package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic

@CompileStatic
class CacheMethods {
    static Map<String, CacheMethod> cacheMethods = [
            "md5sum": new CacheMethodMd5Sum(),
            "md5file" : new CacheMethodMd5File()
    ]

    static CacheMethod getMethod(Object cache) {
        if (cache instanceof CacheMethod)
            return (CacheMethod) cache
        else if (cache instanceof Closure)
            return new CacheMethodDelegate((Closure)cache)
        else
            return cacheMethods[cache.toString()]
    }
}
