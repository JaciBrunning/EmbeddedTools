package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractCacheMethod implements CacheMethod {
    String name

    AbstractCacheMethod(String name) {
        this.name = name
    }

    @Override
    String getName() {
        return name
    }
}
