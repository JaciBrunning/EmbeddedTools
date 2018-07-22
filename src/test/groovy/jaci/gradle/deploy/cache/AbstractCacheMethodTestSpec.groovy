package jaci.gradle.deploy.cache

import spock.lang.Specification

abstract class AbstractCacheMethodTestSpec extends Specification {

    def name = "TEST"
    abstract AbstractCacheMethod getCacheMethod()

    def "named"() {
        expect:
        cacheMethod.getName() == name
    }

}
