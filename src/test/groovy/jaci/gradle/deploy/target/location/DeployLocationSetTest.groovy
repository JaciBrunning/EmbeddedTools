package jaci.gradle.deploy.target.location

import jaci.gradle.deploy.target.RemoteTarget
import spock.lang.Specification
import spock.lang.Subject

class DeployLocationSetTest extends Specification {

    def target = Mock(RemoteTarget)

    @Subject
    def locSet = new DeployLocationSet(target)

    def "starts empty"() {
        expect:
        locSet.empty
    }

    def "add ssh location"() {
        when:
        locSet.ssh { it.user = "myuser" }
        then:
        locSet.size() == 1
        locSet.first() instanceof SshDeployLocation
        locSet.first().user == "myuser"
    }

    def "add dry"() {
        target.isDry() >> true

        when:
        locSet.ssh { it.user = "myuser" }
        then:
        locSet.size() == 1
        locSet.first() instanceof DryDeployLocation
    }

}
