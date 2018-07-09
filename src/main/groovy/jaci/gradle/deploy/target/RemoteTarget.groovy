package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.discovery.DeployLocation
import jaci.gradle.deploy.sessions.context.DeployContext
import org.apache.log4j.Logger
import org.gradle.api.Named
import org.gradle.api.Project

import java.util.concurrent.CountDownLatch

@CompileStatic
class RemoteTarget implements Named {
    final String name
    final Project project

    RemoteTarget(String name, Project project) {
        this.name = name
        this.project = project
        log = Logger.getLogger(toString())
    }

    boolean mkdirs          = true
    boolean discoverInstant = true
    String directory        = null     // Null = default user home
    int timeout             = 3
    boolean failOnMissing   = true
    int maxChannels         = 1

    List<DeployLocation> locations = []

    Closure<Boolean> onlyIf = null  // Delegate: DeployContext

    // Internal
    CountDownLatch latch = new CountDownLatch(1)        // TODO: This should be moved out of here
    private Logger log

    @Override
    String toString() {
        return "RemoteTarget[${name}]".toString()
    }

    boolean enabled(DeployContext ctx) {
        if (onlyIf instanceof Closure) {
            log.debug("OnlyIf...")
            boolean toConnect = ClosureUtils.delegateCall(ctx, onlyIf)
            if (!toConnect) {
                log.debug("OnlyIf check failed! Not connecting...")
                return false
            }
        }
        return true
    }
}