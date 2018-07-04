package jaci.gradle.files

import spock.lang.Specification

class DiscreteDirectoryTreeTest extends Specification {

    def files = ["a", "b", "c"].collect({new File(it)}) as Set

    def "constructor args"() {
        when:
        def ddt = new DiscreteDirectoryTree(files)
        then:
        ddt.getDirectories() == files
    }

    def "add"() {
        def ddt = new DiscreteDirectoryTree()

        when:
        files.each { ddt.add(it) }
        then:
        ddt.getDirectories() == files
    }
}
