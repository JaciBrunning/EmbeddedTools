package jaci.gradle.deploy.target.discovery.action

import groovy.transform.CompileStatic
import jaci.gradle.deploy.context.DefaultDeployContext
import jaci.gradle.deploy.context.DeployContext
import jaci.gradle.deploy.sessions.SshSessionController
import jaci.gradle.deploy.target.discovery.DiscoveryState
import jaci.gradle.deploy.target.location.SshDeployLocation
import jaci.gradle.log.ETLogger
import jaci.gradle.log.ETLoggerFactory

@CompileStatic
class SshDiscoveryAction extends AbstractDiscoveryAction {

    private DiscoveryState state = DiscoveryState.NOT_STARTED
    private ETLogger log

    SshDiscoveryAction(SshDeployLocation dloc) {
        super(dloc)
    }

    @Override
    DiscoveryState getState() {
        return state
    }

    private SshDeployLocation sshLocation() {
        return (SshDeployLocation)getDeployLocation()
    }

    @Override
    DeployContext discover() {
        def location = sshLocation()
        def target = location.target
        def address = location.address
        log = ETLoggerFactory.INSTANCE.create("SshDiscoveryAction[${address}]")

        log.info("Discovery started...")
        state = DiscoveryState.STARTED

        // Split host into host:port, using 22 as the default port if none provided
        def splitHost = address.split(":")
        def hostname = splitHost[0]
        def port = splitHost.length > 1 ? Integer.parseInt(splitHost[1]) : 22
        log.info("Parsed Host: HOST = ${hostname}, PORT = ${port}")

        def resolvedHost = resolveHostname(hostname, location.ipv6)
        state = DiscoveryState.RESOLVED

        def session = new SshSessionController(resolvedHost, port, location.user, location.password, target.timeout, location.target.maxChannels)
        session.open()
        log.info("Found ${resolvedHost}! (${address})")
        state = DiscoveryState.CONNECTED

        def ctx = new DefaultDeployContext(session, log, location, target.directory)
        log.info("Context constructed")

        verify(ctx)
        return ctx
    }

    // TODO: This should be injected to make testing easier.
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
        return "${this.class.simpleName}"
    }
}
