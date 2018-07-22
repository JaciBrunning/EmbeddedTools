package jaci.gradle.files

import groovy.transform.CompileStatic

@CompileStatic
class CombinedDirectoryTree extends AbstractDirectoryTree {

    List<IDirectoryTree> subtrees

    CombinedDirectoryTree() {
        subtrees = []
    }

    CombinedDirectoryTree(IDirectoryTree... trees) {
        subtrees = trees as List
    }

    void add(IDirectoryTree tree) {
        subtrees.add(tree)
    }

    @Override
    Set<File> getDirectories() {
        return subtrees.collectMany { IDirectoryTree t -> t.getDirectories() } as Set<File>
    }
}
