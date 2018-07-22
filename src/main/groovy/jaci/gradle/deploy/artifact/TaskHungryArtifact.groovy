package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic
import org.gradle.api.Task

@CompileStatic
interface TaskHungryArtifact extends Artifact {
    void taskDependenciesAvailable(Set<Task> tasks)
}