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

    // The shared matcher decides what libraries will be deployed
    void setSharedMatchers(List<String> matchers)
    List<String> getSharedMatchers()

    // Library matchers determine what gets passed to the linker as a direct library
    void setLibraryMatchers(List<String> matchers)
    List<String> getLibraryMatchers()

    // Library names determine what gets sent to the linker as a -l flag (good for system libraries / grouped .so)
    void setLibraryNames(List<String> libnames)
    List<String> getLibraryNames()

    void setMaven(String dependencyNotation)
    String getMaven()

    void setFile(File dir_or_zip)
    File getFile()
}
