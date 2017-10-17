package jaci.gradle

import groovy.transform.CompileStatic

@CompileStatic
class ClosureUtils {

    static Object delegateCall(Object object, Closure closure, Object... args) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = object
        return closure.call(args)
    }

}
