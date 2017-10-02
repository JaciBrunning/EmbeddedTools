package jaci.gradle.deployers

import org.gradle.api.NamedDomainObjectSet
import org.gradle.model.Managed
import org.gradle.api.Named
import org.gradle.model.ModelMap
import org.gradle.api.file.FileCollection
import java.util.List

@Managed
interface DeployableStep extends Named {
    void setPredeploy(List<String> precommands)
    List<String> getPredeploy()

    void setPostdeploy(List<String> postcommands)
    List<String> getPostdeploy()

    void setOrder(int order)
    int getOrder()
}

@Managed
interface Deployer extends DeployableStep {
    void setTargets(List<String> targets)
    List<String> getTargets()

    void setUser(String user)
    String getUser()

    void setPassword(String pass)
    String getPassword()

    void setPromptPassword(boolean prompt)
    boolean getPromptPassword()

    ModelMap<ArtifactBase> getArtifacts()
}

@Managed
interface ArtifactBase extends DeployableStep {
    void setDirectory(String directory)
    String getDirectory()
}

@Managed 
interface FileArtifact extends ArtifactBase {
    void setFile(File file)
    File getFile()

    void setFilename(String filename)
    String getFilename()
    
    void setCache(boolean enableCache)
    boolean getCache()
}

@Managed
interface FileSetArtifact extends ArtifactBase {
    void setFiles(Set<File> files)
    Set<File> getFiles()

    void setCache(boolean enableCache)
    boolean getCache()
}

@Managed
interface JavaArtifact extends FileArtifact { }

@Managed 
interface NativeArtifact extends FileArtifact {
    void setLibraries(boolean deploySharedLibraries)
    boolean getLibraries()

    void setLibrootdir(String libRootDir)
    String getLibrootdir()

    void setLibrarycache(boolean cacheSharedLibraries)
    boolean getLibrarycache()

    void setPlatform(String platform)
    String getPlatform()
}

@Managed
interface CommandArtifact extends ArtifactBase {
    void setCommand(String command)
    String getCommand()
}

@Managed
interface DeployersSpec extends ModelMap<Deployer> { }