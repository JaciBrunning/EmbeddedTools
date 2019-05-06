package jaci.gradle.deploy.target.discovery

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

@CompileStatic
@InheritConstructors(constructorAnnotations = true)
class TargetNotFoundException extends RuntimeException {
}
