package jaci.gradle

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
class SortUtils {

    @Canonical
    static class TopoMember<T> {
        String name
        List<String> dependsOn = []
        T extra = null
        protected int mark = 0
    }

    static class CyclicDependencyException extends RuntimeException {
        CyclicDependencyException(TopoMember member) {
            super("Cyclic dependency! ${member.name} : ${member.dependsOn.join(', ')}")
        }
    }

    static <T> List<TopoMember<T>> topoSort(List<TopoMember<T>> members) {
        List<TopoMember<T>> unmarked = members
        List<TopoMember<T>> sorted = []
        while ((unmarked = unmarked.findAll { TopoMember member -> member.mark == 0 }).size() > 0) {
            _visit(unmarked.first(), members, sorted)
        }
        return sorted
    }

    static void _visit(TopoMember member, List<TopoMember> members, List<TopoMember> sorted) {
        if (member.mark == 1) throw new CyclicDependencyException(member)   // Temp mark
        if (member.mark == 2) return                                        // Perm mark
        member.mark = 1
        member.dependsOn.each { String dep ->
            def m = members.find { TopoMember mem -> mem.name == dep }
            if (m != null)
                _visit(m, members, sorted)
        }
        member.mark = 2
        sorted << member
    }

}
