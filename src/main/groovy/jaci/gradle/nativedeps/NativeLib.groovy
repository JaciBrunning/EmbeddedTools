package jaci.gradle.nativedeps

import groovy.transform.CompileStatic
import org.gradle.model.Managed

@Managed
@CompileStatic
interface NativeLib extends BaseLibSpec {
    void setHeaderDirs(List<String> dirs)
    List<String> getHeaderDirs()

    void setSourceDirs(List<String> dirs)
    List<String> getSourceDirs()

    // Static Libraries to be linked during compile time
    void setStaticMatchers(List<String> matchers)
    List<String> getStaticMatchers()

    // Shared Libraries to be linked during compile time
    void setSharedMatchers(List<String> matchers)
    List<String> getSharedMatchers()

    // Parts of the libraries that are needed for debug
    void setDebugMatchers(List<String> matchers)
    List<String> getDebugMatchers()

    // Libraries that aren't linked during compile time, but still necessary for the
    // program to run (loose dynamic deps)
    void setDynamicMatchers(List<String> matchers)
    List<String> getDynamicMatchers()

    // Static Libraries to be excluded during compile time
    void setStaticExcludes(List<String> excludes)
    List<String> getStaticExcludes()

    // Shared Libraries to be excluded during compile time
    void setSharedExcludes(List<String> excludes)
    List<String> getSharedExcludes()

    // Parts of the libraries that are excluded for debug
    void setDebugExcludes(List<String> excludes)
    List<String> getDebugExcludes()

    // Libraries that aren't linked during compile time, but still necessary for the
    // program to run (loose dynamic deps)
    void setDynamicExcludes(List<String> excludes)
    List<String> getDynamicExcludes()

    // Library names determine what gets sent to the linker as a -l flag (good for system libraries / grouped .so)
    void setSystemLibs(List<String> libnames)
    List<String> getSystemLibs()

    void setMaven(String dependencyNotation)
    String getMaven()

    // Specify which dependency configuration to plop this library in.
    // By default, it is generated as "native_${name}"
    void setConfiguration(String name)
    String getConfiguration()

    void setFile(File dir_or_zip)
    File getFile()
}
