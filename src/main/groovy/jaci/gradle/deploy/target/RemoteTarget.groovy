package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.location.DeployLocation
import jaci.gradle.deploy.target.location.DeployLocationSet
import org.apache.log4j.Logger
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named
import org.gradle.api.Project

@CompileStatic
class RemoteTarget implements Named {
    final String name
    final Project project
    private Logger log

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

    DomainObjectSet<DeployLocation> locations = new DeployLocationSet(this)

    Closure<Boolean> onlyIf = null  // Delegate: DeployContext

    def locations(final Closure closure) {
        project.configure(locations as Object, closure)
    }

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