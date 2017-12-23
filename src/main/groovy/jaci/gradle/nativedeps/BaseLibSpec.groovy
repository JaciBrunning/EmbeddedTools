package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.api.Named
import org.gradle.model.Managed

@Managed
@CompileStatic
interface BaseLibSpec extends Named {
    // Overrides the 'named' status for this object.
    // This doesn't have to be unique, in the case it is not,
    // it will be added to an existing PrebuiltLibrary
    void setMainLibraryName(String libraryName)
    String getMainLibraryName()

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