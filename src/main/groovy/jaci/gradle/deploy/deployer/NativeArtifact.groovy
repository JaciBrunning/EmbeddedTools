package jaci.gradle.deploy.deployer

import groovy.transform.CompileStatic
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.cache.Cacheable
import org.gradle.api.Project

@CompileStatic
class NativeArtifact extends FileArtifact implements Cacheable {
    NativeArtifact(String name) {
        super(name)
        component = name
    }

    String component = null
    String targetPlatform = null
}
