package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic

import javax.inject.Inject

@CompileStatic
abstract class AbstractCacheMethod implements CacheMethod {
    String name

    @Inject
    AbstractCacheMethod(String name) {
        this.name = name
    }

    @Override
    String getName() {
        return name
    }
}
