package jaci.gradle.files

import groovy.transform.CompileStatic
import org.gradle.api.file.FileTree

@CompileStatic
abstract class AbstractDirectoryTree {

    abstract Set<File> getDirectories()

    AbstractDirectoryTree plus(AbstractDirectoryTree other) {
        return new CombinedDirectoryTree().add(this).add(other)
    }

}
