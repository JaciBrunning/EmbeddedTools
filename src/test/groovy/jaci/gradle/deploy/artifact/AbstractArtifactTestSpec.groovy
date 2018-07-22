package jaci.gradle.deploy.artifact

import groovy.transform.InheritConstructors
import jaci.gradle.deploy.context.DeployContext
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Subject

abstract class AbstractArtifactTestSpec extends Specification {

    def project = ProjectBuilder.builder().build()
    def name = "TEST"

    abstract AbstractArtifact getArtifact()

    def "basic getters"() {
        expect:
        artifact.getName() == name
        artifact.getProject() == project
    }

    def "dependencies"() {
        expect:
        artifact.getDependencies().empty
        artifact.getTargets().empty

        when:
        artifact.dependsOn("a", "b", "c")
        then:
        artifact.getDependencies().toSet().equals(["a", "b", "c"] as Set<Object>)
    }

    def "disabling"() {
        when:
        artifact.setDisabled()
        then:
        artifact.isDisabled()
        !artifact.isEnabled(null)

        when:
        artifact.setDisabled(false)
        then:
        !artifact.isDisabled()
        artifact.isEnabled(null)
    }

    def "onlyIf"() {
        artifact.onlyIf = { false }

        when:
        def e = artifact.isEnabled(null)
        then:
        !e
    }

}
