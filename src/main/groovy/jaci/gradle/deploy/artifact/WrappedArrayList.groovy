package jaci.gradle.deploy.artifact

import org.gradle.api.Action
import jaci.gradle.ActionWrapper
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

// An ArrayList of actions, with overridding the leftShift operator
// to take a closure. Enables closures from the DSL
@CompileStatic
@InheritConstructors
class WrappedArrayList<T> extends ArrayList<Action<T>> {
  WrappedArrayList leftShift(Closure closure) {
    Action<T> wrapper = new ActionWrapper<T>(closure)
    this.add(wrapper)
    return this
  }
}
