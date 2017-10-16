package jaci.gradle.nativedeps

import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.AbstractFileCollection

class PreemptiveDirectoryFileCollection extends AbstractFileCollection {

    FileTree parentTree
    List<String> subDirs

    public PreemptiveDirectoryFileCollection(FileTree parentTree, List<String> subDirs) {
        this.parentTree = parentTree
        this.subDirs = subDirs
    }

    @Override
    String getDisplayName() {
        return "Preemptive Directory File Collection for ${parentTree.tree.mirror.dir}"
    }

    @Override
    Set<File> getFiles() {
        return subDirs.collect { new File(parentTree.asFileTrees.first().dir, it) }
    }

    Set<File> getPreemptive() {
        return subDirs.collect { new File(parentTree.tree.mirror.dir, it) }
    }
}
