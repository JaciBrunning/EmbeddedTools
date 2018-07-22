package jaci.gradle

import spock.lang.Specification

class SortUtilsTest extends Specification {

    def memberA = new SortUtils.TopoMember(name: "A")
    def memberB = new SortUtils.TopoMember(name: "B", dependsOn: ["A"])
    def memberC = new SortUtils.TopoMember(name: "C")
    def memberD = new SortUtils.TopoMember(name: "D", dependsOn: ["B", "C"])

    def allMembers = [ memberA, memberB, memberC, memberD ]

    def "sort dependencies"() {
        when:
        def sorted = SortUtils.topoSort(allMembers)

        then:
        sorted.indexOf(memberA) < sorted.indexOf(memberB)
        sorted.indexOf(memberB) < sorted.indexOf(memberD)
        sorted.indexOf(memberC) < sorted.indexOf(memberD)
    }

    def "cyclic"() {
        def member0 = new SortUtils.TopoMember(name: "0", dependsOn: ["1"])
        def member1 = new SortUtils.TopoMember(name: "1", dependsOn: ["0"])
        allMembers += [member0, member1]

        when:
        SortUtils.topoSort(allMembers)

        then:
        thrown SortUtils.CyclicDependencyException
    }

}
