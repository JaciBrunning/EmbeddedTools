package jaci.gradle.deploy.artifact

import groovy.transform.CompileStatic

@CompileStatic
interface ConfigurableArtifact extends Artifact {
    List<Closure> getPredeploy()
    void setPredeploy(List<Closure> predeploys)

    List<Closure> getPostdeploy()
    void setPostdeploy(List<Closure> postdeploys)

    Closure<Boolean> getOnlyif()
    void setOnlyif(Closure<Boolean> checkClosure)

    void setDirectory(String directory)
}