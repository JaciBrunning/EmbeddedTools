package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.target.location.DeployLocation
import jaci.gradle.deploy.target.location.DeployLocationSet
import org.apache.log4j.Logger
import org.gradle.api.DomainObjectSet
import org.gradle.api.Named
import org.gradle.util.ConfigureUtil

@CompileStatic
class RemoteTarget implements Named {
    final String name
    private Logger log

    RemoteTarget(String name) {
        this.name = name
        log = Logger.getLogger(toString())
    }

    boolean mkdirs          = true
    String directory        = null     // Null = default user home
    int timeout             = 3
    boolean failOnMissing   = true
    int maxChannels         = 1

    boolean dry             = false

    DomainObjectSet<DeployLocation> locations = new DeployLocationSet(this)

    Closure<Boolean> onlyIf = null  // Delegate: DeployContext

    def locations(final Closure closure) {
        ConfigureUtil.configure(closure, (Object)locations)
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