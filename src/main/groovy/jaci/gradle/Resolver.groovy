package jaci.gradle

import groovy.transform.CompileStatic

@CompileStatic
interface Resolver<T> {
    T resolve(Object o)
}