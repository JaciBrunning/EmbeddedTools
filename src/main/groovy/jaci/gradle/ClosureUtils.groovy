package jaci.gradle
// We can't make this @CompileStatic as the splat (*) operator does not work in static typechecking
// mode.
class ClosureUtils {

    static Object delegateCall(Object object, Closure closure, Object... args) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = object
        return closure.call(object, *args)
    }

}
