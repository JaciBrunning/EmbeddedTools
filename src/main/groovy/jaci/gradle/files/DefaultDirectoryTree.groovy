package jaci.gradle.files

import groovy.transform.CompileStatic
import org.gradle.api.file.DirectoryTree
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.file.collections.FileSystemMirroringFileTree
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.LocalFileTree

import java.util.function.Supplier

@CompileStatic
class DefaultDirectoryTree extends AbstractDirectoryTree {

    private Supplier<FileTree> treeSupplier
    private List<String> subdirs

    DefaultDirectoryTree(Supplier<FileTree> rootTree, List<String> subdirs) {
        this.treeSupplier = rootTree
        this.subdirs = subdirs
    }

    DefaultDirectoryTree(FileTree tree, List<String> subdirs) {
        this({ tree }, subdirs)
    }

    @Override
    Set<File> getDirectories() {
        Set<File> files = subdirs.collect { String subdir ->
            File rootDir = null
            def tree = treeSupplier.get()
            if (tree instanceof DirectoryTree) {                // project.fileTree
                rootDir = ((DirectoryTree)tree).getDir()
            } else if (tree instanceof FileTreeAdapter) {       // project.zipTree
                // Recreation of FileTreeAdapter.getAsFileTrees() (protected method)
                def lTree = (tree as FileTreeAdapter).tree
                // We have to visit in order to force ziptrees to extract their contents
                (tree as FileTreeAdapter).visit( { FileVisitDetails details -> details.getFile() } )
                DirectoryTree dirTree
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
