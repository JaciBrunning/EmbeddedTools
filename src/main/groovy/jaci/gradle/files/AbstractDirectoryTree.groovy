package jaci.gradle.files

import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractDirectoryTree implements IDirectoryTree {

    IDirectoryTree plus(IDirectoryTree other) {
        return new CombinedDirectoryTree(this, other)
    }

}
