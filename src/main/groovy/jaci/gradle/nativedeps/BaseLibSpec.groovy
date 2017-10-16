package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.api.Named
import org.gradle.model.Managed

@Managed
@CompileStatic
interface BaseLibSpec extends Named {
    void setTargetPlatform(String platforms)
    String getTargetPlatform()

    void setFlavor(String flavour)
    String getFlavor()

    void setBuildType(String type)
    String getBuildType()
}