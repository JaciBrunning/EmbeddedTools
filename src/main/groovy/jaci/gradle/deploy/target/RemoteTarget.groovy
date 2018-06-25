package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import jaci.gradle.ClosureUtils
import jaci.gradle.EmbeddedTools
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.tasks.TargetDiscoveryTask
import jaci.gradle.transport.SshSessionController
import org.apache.log4j.Logger
import org.gradle.api.Named
import org.gradle.api.Project

import java.util.concurrent.CountDownLatch

@CompileStatic
@EqualsAndHashCode(includes = 'name')
class RemoteTarget implements Named {
    final String name

    RemoteTarget(String name) {
        this.name = name
        log = Logger.getLogger(toString())
    }

    List<String> addresses  = []
    boolean mkdirs          = true
    boolean ipv6            = false    // Enable IPv6 resolution? (experimental)
    boolean discoverInstant = true
    String directory        = null     // Null = default user home
    String user             = null
    String password         = ""
    boolean promptPassword  = false
    int timeout             = 3
    boolean failOnMissing   = true
    int maxChannels = 1

    List<Closure> precheck  = []  // Called before onlyIf
    Closure<Boolean> onlyIf = null  // Delegate: DeployContext

    Logger log

    // Internal
    CountDownLatch latch = new CountDownLatch(1)

    @Override
    String toString() {
        return "RemoteTarget[${name}]".toString()
    }

    boolean toConnect(DeployContext ctx) {
        log.debug("Precheck....")
        precheck.forEach { Closure c -> ClosureUtils.delegateCall(ctx, c) }

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