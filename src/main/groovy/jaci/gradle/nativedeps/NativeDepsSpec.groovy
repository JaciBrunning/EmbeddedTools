package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.model.Managed
import org.gradle.model.ModelMap

@Managed
@CompileStatic
interface NativeDepsSpec extends ModelMap<BaseLibSpec> { }