package jaci.gradle

import groovy.transform.CompileStatic
import java.util.function.Predicate
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.ClosureUtils

/**
 * A simple class to wrap a Closure in an Predicate
 * Groovy's cast will never delegate the closure
 */
@CompileStatic
class PredicateWrapper<T> implements Predicate {
  private Closure closure

  PredicateWrapper(Closure closure) {
    this.closure = closure
  }

  boolean test(T t) {
    return ClosureUtils.delegateCall(t, closure)
  }
}
