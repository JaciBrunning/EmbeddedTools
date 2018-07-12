package jaci.gradle.deploy.target.discovery.action

import groovy.transform.CompileStatic
import jaci.gradle.ETLogger
import jaci.gradle.deploy.context.DefaultDeployContext
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.sessions.SshSessionController
import jaci.gradle.deploy.target.discovery.DiscoveryState
import jaci.gradle.deploy.target.location.SshDeployLocation
import org.gradle.api.internal.project.ProjectInternal

@CompileStatic
class SshDiscoveryAction extends AbstractDiscoveryAction {

    private DiscoveryState state
    private SshDeployLocation location
    private ETLogger log

    SshDiscoveryAction(SshDeployLocation dloc) {
        super(dloc)
        log = new ETLogger(toString(), ((ProjectInternal)location.target.project).services) // TODO we should have a factory for this
    }

    @Override
    DiscoveryState getState() {
        return state
    }

    @Override
    DeployContext discover() {
        def target = location.target

        state = DiscoveryState.STARTED
        log.info("Discovery started...")

        // Split host into host:port, using 22 as the default port if none provided
        def splitHost = location.address.split(":")
        def hostname = splitHost[0]
        def port = splitHost.length > 1 ? Integer.parseInt(splitHost[1]) : 22
        log.info("Parsed Host: HOST = ${hostname}, PORT = ${port}")

        def resolvedHost = resolveHostname(hostname, location.ipv6)
        state = DiscoveryState.RESOLVED

        def session = new SshSessionController(resolvedHost, port, location.user, location.password, target.timeout, location.target.maxChannels)
        session.open()
        log.info("Found ${resolvedHost}! (${location.address})")
        state = DiscoveryState.CONNECTED

        def ctx = new DefaultDeployContext(session, log, location, target.directory)
        log.info("Context constructed")

        return ctx
    }

    private String resolveHostname(String hostname, boolean allowIpv6) {
        String resolvedHost = hostname
        boolean hasResolved = false
        for (InetAddress addr : InetAddress.getAllByName(hostname)) {
            if (!addr.isMulticastAddress()) {
                if (!allowIpv6 && addr instanceof Inet6Address) {
                    log.info("Resolved address ${addr.getHostAddress()} ignored! (IPv6)")
                } else {
                    log.info("Resolved ${addr.getHostAddress()}")
                    resolvedHost = addr.getHostAddress()
                    hasResolved = true
                    break;
                }
            }
        }

        if (!hasResolved)
            log.info("No host resolution! Using original...")

        return resolvedHost
    }

    @Override
    public String toString() {
        return "${this.class.simpleName}[${this.location.address}]"
    }
}
