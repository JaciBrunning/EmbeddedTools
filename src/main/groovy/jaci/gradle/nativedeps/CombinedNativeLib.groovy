package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.model.Managed

@Managed
@CompileStatic
interface CombinedNativeLib extends BaseLibSpec {
    void setLibs(List<String> libs)
    List<String> getLibs()
}