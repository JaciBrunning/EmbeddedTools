package jaci.gradle.deploy.target.discovery

import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.sessions.SessionController
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.discovery.action.DiscoveryAction
import jaci.gradle.deploy.target.location.DeployLocation
import org.gradle.api.internal.DefaultDomainObjectSet
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Consumer

class TargetDiscoveryWorkerTest extends Specification {

    def target = Mock(RemoteTarget) {
        getTimeout() >> 1
        getLocations() >> new DefaultDomainObjectSet(DeployLocation.class)
    }
    def context = Mock(DeployContext) {
        getController() >> Mock(SessionController)
    }
    def exception_cause = Mock(Exception)
    def callback = Mock(Consumer)

    def location_success = Mock(DeployLocation) { loc ->
        loc.getTarget() >> target
        loc.createAction() >> Mock(DiscoveryAction) {
            discover() >> context
            getException() >> null
            getState() >> DiscoveryState.CONNECTED
            getDeployLocation() >> loc
        }
    }

    def location_failure = Mock(DeployLocation) { loc ->
        loc.getTarget() >> target
        loc.createAction() >> Mock(DiscoveryAction) { act ->
            act.discover() >> null
            act.getException() >> new DiscoveryFailedException(act, exception_cause)
            act.getState() >> DiscoveryState.RESOLVED
            act.getDeployLocation() >> loc
        }
    }

    @Subject
    def worker = new TargetDiscoveryWorker(target, callback)

    def "single success"() {
        target.getLocations().add(location_success)

        when:
        worker.run()
        then:
        1 * callback.accept(context)
        0 * callback.accept(_)
    }

    def "single failure"() {
        target.getLocations().add(location_failure)

        when:
        worker.run()
        then:
        1 * callback.accept(null)
        0 * callback.accept(_)
    }

}
