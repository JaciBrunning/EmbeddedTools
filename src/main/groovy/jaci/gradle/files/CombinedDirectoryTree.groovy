package jaci.gradle.files

import groovy.transform.CompileStatic

@CompileStatic
class CombinedDirectoryTree extends AbstractDirectoryTree {

    List<AbstractDirectoryTree> subtrees

    CombinedDirectoryTree() {
        subtrees = []
    }

    CombinedDirectoryTree add(AbstractDirectoryTree tree) {
        subtrees.add(tree)
        return this
    }

    @Override
    Set<File> getDirectories() {
        return subtrees.collectMany { AbstractDirectoryTree t -> t.getDirectories() } as Set<File>
    }
}
