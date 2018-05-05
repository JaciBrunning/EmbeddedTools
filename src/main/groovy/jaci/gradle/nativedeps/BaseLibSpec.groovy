package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.api.Named
import org.gradle.model.Managed

@Managed
@CompileStatic
interface BaseLibSpec extends Named {
    // Overrides the 'named' status for this object.
    // This is primarily used for cases where there are more
    // than one version of a library, for different platforms
    void setLibraryName(String libraryName)
    String getLibraryName()

    void setTargetPlatform(String platforms)
    String getTargetPlatform()

    // Same as above, but for multiple platforms. Appends platform name
    // to the binary names
    void setTargetPlatforms(List<String> platforms_multi)
    List<String> getTargetPlatforms()

    void setFlavor(String flavour)
    String getFlavor()

    void setBuildType(String type)
    String getBuildType()
}