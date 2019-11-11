package jaci.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.ClosureUtils

/**
 * A simple class to wrap a Closure in an Action
 * Groovy's cast will never delegate the closure
 */
@CompileStatic
class ActionWrapper<T> implements Action<T> {
  private Closure closure

  ActionWrapper(Closure closure) {
    this.closure = closure
  }

  void execute(T t) {
    ClosureUtils.delegateCall(t, closure)
  }
}
