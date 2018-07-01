package jaci.gradle.files

import groovy.transform.CompileStatic
import org.gradle.api.file.DirectoryTree
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.file.collections.DirectoryFileTree
import org.gradle.api.internal.file.collections.FileSystemMirroringFileTree
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.LocalFileTree

@CompileStatic
class DefaultDirectoryTree extends AbstractDirectoryTree {

    private FileTree tree
    private List<String> subdirs

    DefaultDirectoryTree(FileTree rootTree, List<String> subdirs) {
        this.tree = rootTree
        this.subdirs = subdirs
    }

    @Override
    Set<File> getDirectories() {
        Set<File> files = subdirs.collect { String subdir ->
            File rootDir = null
            if (tree instanceof DirectoryTree) {                // project.fileTree
                rootDir = (tree as DirectoryTree).getDir()
            } else if (tree instanceof FileTreeAdapter) {       // project.zipTree
                // Recreation of FileTreeAdapter.getAsFileTrees() (protected method)
                def lTree = (tree as FileTreeAdapter).tree
                // We have to visit in order to force ziptrees to extract their contents
                (tree as FileTreeAdapter).visit( { FileVisitDetails details -> details.getFile() } )
                DirectoryFileTree dirTree
                if (lTree instanceof FileSystemMirroringFileTree)
                    dirTree = (lTree as FileSystemMirroringFileTree).mirror
                else
                    dirTree = (lTree as LocalFileTree).getLocalContents().first()
                rootDir = dirTree.dir
            }

            new File(rootDir, subdir)
        } as Set<File>
        return files
    }
}
