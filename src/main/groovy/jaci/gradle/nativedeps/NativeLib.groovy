package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.model.Managed

@Managed
@CompileStatic
interface NativeLib extends BaseLibSpec {
    void setHeaderDirs(List<String> dirs)
    List<String> getHeaderDirs()

    void setStaticMatchers(List<String> matchers)
    List<String> getStaticMatchers()

    void setSharedMatchers(List<String> matchers)
    List<String> getSharedMatchers()

    void setAddLinkerArgs(boolean addLinkerArgs)
    boolean getAddLinkerArgs()

    void setMaven(String dependencyNotation)
    String getMaven()

    void setFile(File dir_or_zip)
    File getFile()
}
