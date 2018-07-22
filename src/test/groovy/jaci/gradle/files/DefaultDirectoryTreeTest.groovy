package jaci.gradle.files

import org.gradle.api.file.DirectoryTree
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.MinimalFileTree
import spock.lang.Specification

class DefaultDirectoryTreeTest extends Specification {

    def rootDir = new File("rootDir")
    def subdirs = ["a", "b"]
    def expected = subdirs.collect { new File(rootDir, it) } as Set

    def "local directory tree"() {
        def tree = Mock(DirectoryFileTree) {
            getDir() >> rootDir
        }
        def dt = new DefaultDirectoryTree(tree, subdirs)

        when:
        def dirs = dt.getDirectories()

        then:
        dirs == expected
    }

    def "adapter directory tree early visit"() {
        def minimal = Mock(MinimalFileTree)
        def fta = new FileTreeAdapter(minimal)

        // Ensure we don't visit the file collection during construction,
        // as this happens during project configuration and will cause
        // eager extraction
        when:
        def dt = new DefaultDirectoryTree(fta, subdirs)
        then:
        0 * minimal.visit(_)
    }

    // We can't unit test getDirectories() for adapter types since one of the interfaces
    // needs to return a class that uses a service that we don't have access to in unit

    static interface DirectoryFileTree extends FileTree, DirectoryTree {}

}
