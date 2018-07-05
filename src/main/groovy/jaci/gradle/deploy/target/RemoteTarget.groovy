package jaci.gradle.deploy.target

import groovy.transform.CompileStatic
import jaci.gradle.ClosureUtils
import jaci.gradle.deploy.context.DeployContext
import org.apache.log4j.Logger
import org.gradle.api.Named

import java.util.concurrent.CountDownLatch

@CompileStatic
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