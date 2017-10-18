package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class TargetsExtension extends HashMap<String, RemoteTarget> {
    Project project
    TargetsExtension(Project project) {
        this.project = project
    }

    def target(String name, Class<? extends RemoteTarget> type, final Closure config) {
        def target = type.newInstance(name)
        project.configure(target, config)
        this[name] = target
    }

    def target(String name, final Closure config) {
        target(name, RemoteTarget, config)
    }
}
