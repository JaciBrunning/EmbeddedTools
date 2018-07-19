package jaci.gradle.deploy.target.location

import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.discovery.action.SshDiscoveryAction
import spock.lang.Specification
import spock.lang.Subject

class SshDeployLocationTest extends Specification {

    def target = Mock(RemoteTarget)

    @Subject
    def location = new SshDeployLocation(target)

    def "get target"() {
        expect:
        location.getTarget() == target
    }

    def "assert address not null"() {
        location.user = "not null"

        when:
        location.createAction()
        then:
        thrown(IllegalArgumentException)
    }

    def "assert user not null"() {
        location.address = "not null"

        when:
        location.createAction()
        then:
        thrown(IllegalArgumentException)
    }

    def "creates discovery action"() {
        location.user = "not null"
        location.address = "not null"

        when:
        def a = location.createAction()
        then:
        a instanceof SshDiscoveryAction
        a.deployLocation == location
    }

}
