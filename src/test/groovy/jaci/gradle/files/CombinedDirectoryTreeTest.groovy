package jaci.gradle.files

import spock.lang.Specification
import spock.lang.Subject

class CombinedDirectoryTreeTest extends Specification {

    def files1 = [ "a", "b", "1/a", "1/b" ].collect({new File(it)}) as Set
    def files2 = [ "a", "c", "2/a", "2/c" ].collect({new File(it)}) as Set
    def allFiles = (files1 + files2) as Set

    @Subject AbstractDirectoryTree dt1 = new DiscreteDirectoryTree(files1)
    @Subject AbstractDirectoryTree dt2 = new DiscreteDirectoryTree(files2)

    def "add"() {
        def cdt = new CombinedDirectoryTree()

        when:
        cdt.add(dt1)
        cdt.add(dt2)

        then:
        cdt.getDirectories() == allFiles
    }

    def "constructor args"() {
        when:
        def cdt = new CombinedDirectoryTree(dt1, dt2)
        then:
        cdt.getDirectories() == allFiles
    }

    def "addition of objects (generic)"() {
        when:
        def result = dt1 + dt2
        then:
        result.getDirectories() == allFiles
    }

}
