package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import jaci.gradle.deploy.cache.Cacheable

@CompileStatic
class NativeArtifact extends FileArtifact implements Cacheable {
    NativeArtifact(String name) {
        super(name)
        component = name
    }

    String component = null
    String targetPlatform = null
}
