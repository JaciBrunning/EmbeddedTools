package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.Action
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.ClosureUtils

/**
 * A simple class to wrap a Closure in an Action
 * Groovy's cast will never delegate the closure
 */
@CompileStatic
class ActionWrapper implements Action<DeployContext> {
  private Closure closure

  ActionWrapper(Closure closure) {
    this.closure = closure
  }

  void execute(DeployContext ctx) {
    ClosureUtils.delegateCall(ctx, closure)
  }
}
