package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.model.Managed

@Managed
@CompileStatic
interface BaseLibSpec {
    void setTargetPlatform(String platforms)
    String getTargetPlatform()

    void setFlavor(String flavour)
    String getFlavor()

    void setBuildType(String type)
    String getBuildType()
}