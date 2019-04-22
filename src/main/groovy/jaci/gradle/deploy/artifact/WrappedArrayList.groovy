package jaci.gradle.deploy.artifact

import org.gradle.api.Action
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.deploy.context.DeployContext

// An ArrayList of actions, with overridding the leftShift operator
// to take a closure. Enables closures from the DSL
@CompileStatic
@InheritConstructors
class WrappedArrayList extends ArrayList<Action<DeployContext>> {
  WrappedArrayList leftShift(Closure closure) {
    Action<DeployContext> wrapper = new ActionWrapper(closure)
    this.add(wrapper)
    return this
  }
}
