package jaci.gradle.files

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileTree

import java.util.function.Supplier
import java.util.function.Function

@CompileStatic
class FileTreeSupplier implements Supplier<FileTree> {
  private Set<ResolvedArtifact> resolvedArtifacts
  private Function<Set<ResolvedArtifact>, FileTree> resolveFunc
  private Configuration cfg

  FileTreeSupplier(Configuration cfg, Function<Set<ResolvedArtifact>, FileTree> resolveFunc) {
    this.cfg = cfg
    this.resolveFunc = resolveFunc
  }

  FileTree get() {
    if (resolvedArtifacts == null) {
      resolvedArtifacts = cfg.resolvedConfiguration.resolvedArtifacts
    }
    return resolveFunc.apply(resolvedArtifacts)
  }
}
