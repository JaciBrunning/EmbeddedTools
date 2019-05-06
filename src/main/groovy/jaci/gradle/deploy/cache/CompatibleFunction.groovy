package jaci.gradle.deploy.cache

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DeployContext

@CompileStatic
@FunctionalInterface
interface CompatibleFunction {
  boolean check(DeployContext ctx)
}
