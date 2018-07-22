package jaci.gradle

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

@CompileStatic
interface Resolver<T> {
    T resolve(Object o)

    @CompileStatic
    @InheritConstructors
    static class ResolveFailedException extends RuntimeException {}
}