package jaci.gradle.deploy.target.discovery

import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.discovery.action.AbstractDiscoveryAction
import jaci.gradle.deploy.target.discovery.action.DiscoveryAction
import jaci.gradle.deploy.target.location.AbstractDeployLocation
import jaci.gradle.deploy.target.location.DeployLocation
import jaci.gradle.deploy.target.location.DeployLocationSet
import org.gradle.api.internal.DefaultDomainObjectSet
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Consumer

class TargetDiscoveryWorkerTest extends Specification {

    def target = Mock(RemoteTarget) { RemoteTarget t ->
        t.getTimeout() >> 1
        t.getLocations() >> new DeployLocationSet(t)
    }
    def callback = Mock(Consumer)
    def context = Mock(DeployContext)

    @Subject
    def worker = new TargetDiscoveryWorker(target, callback)

    def "single success"() {
        target.getLocations().add(new MockedLocation(target, context, true))

        when:
        worker.run()
        then:
        1 * callback.accept(context)
        0 * callback.accept(null)
    }

    def "single failure"() {
        target.getLocations().add(new MockedLocation(target, context, false))

        when:
        worker.run()
        then:
        1 * callback.accept(null)
        0 * callback.accept(_)
    }

    def "success + failure"() {
        target.getLocations().add(new MockedLocation(target, context, false))
        target.getLocations().add(new MockedLocation(target, context, true))

        // Should say we found the target
        when:
        worker.run()
        then:
        1 * callback.accept(context)
        0 * callback.accept(null)
    }

    def "all success"() {
        target.getLocations().add(new MockedLocation(target, context, true))
        target.getLocations().add(new MockedLocation(target, context, true))

        when:
        worker.run()
        then:
        1 * callback.accept(context)
        0 * callback.accept(null)
    }

    def "all failure"() {
        target.getLocations().add(new MockedLocation(target, context, false))
        target.getLocations().add(new MockedLocation(target, context, false))

        when:
        worker.run()
        then:
        1 * callback.accept(null)
        0 * callback.accept(_)
    }

    def "storage"() {
        TargetDiscoveryWorker.clearStorage()
        target.getLocations().add(new MockedLocation(target, context, true))

        when:
        def hc = TargetDiscoveryWorker.submitStorage(target, callback)
        then:
        TargetDiscoveryWorker.storageCount() == 1

        // Check that, after construction, it is removed from that map
        // and its attributes match
        when:
        def worker = new TargetDiscoveryWorker(hc)
        then:
        TargetDiscoveryWorker.storageCount() == 0
        worker.target == target
        worker.callback == callback
    }

    static class MockedLocation extends AbstractDeployLocation {
        boolean success
        DeployContext ctx

        MockedLocation(RemoteTarget target, DeployContext ctx, boolean success) {
            super(target)
            this.success = success
            this.ctx = ctx
        }

        @Override
        DiscoveryAction createAction() {
            return new MockedAction(this, ctx, success)
        }

        @Override
        String friendlyString() {
            return "mock'd"
        }
    }


    static class MockedAction extends AbstractDiscoveryAction {
        boolean success
        DeployContext ctx

        MockedAction(DeployLocation location, DeployContext ctx, boolean success) {
            super(location)
            this.success = success
            this.ctx = ctx
        }

        @Override
        DeployContext discover() {
            if (!success) throw new DiscoveryFailedException(this, new RuntimeException())
            return ctx
        }

        @Override
        DiscoveryState getState() {
            return DiscoveryState.CONNECTED
        }
    }

}
