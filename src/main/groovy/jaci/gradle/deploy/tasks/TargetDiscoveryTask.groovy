package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.WorkerStorage
import jaci.gradle.deploy.DefaultDeployContext
import jaci.gradle.deploy.DeployContext
import jaci.gradle.deploy.DeployLogger
import jaci.gradle.deploy.DryDeployContext
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.transport.SshSessionController
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory

import javax.inject.Inject
import java.util.concurrent.TimeUnit

@CompileStatic
class TargetDiscoveryTask extends DefaultTask {

    private static class AddressStorage {
        String address
        int port
        String target

        AddressStorage(String addr, int port, String target) {
            this.address = addr;
            this.port = port
            this.target = target;
        }
    }

    @Internal
    final WorkerExecutor workerExecutor
    static WorkerStorage<RemoteTarget>   targetStorage  = WorkerStorage.obtain()
    static WorkerStorage<AddressStorage> addressStorage = WorkerStorage.obtain()

    @Internal
    DeployLogger            log
    @Internal
    SshSessionController    session
    @Internal
    DeployContext           context

    @Input
    RemoteTarget target

    @Inject
    TargetDiscoveryTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    String targetFullName() {
        return "${project}:${target.name}"
    }

    @TaskAction
    void discoverTarget() {
        // Ask for password if needed
        log = new DeployLogger(0)

        if (EmbeddedTools.isDryRun(project)) {
            log.log("Dry Run! Using ${target.addresses.first()} for target ${target.name}")
            addressStorage << new AddressStorage(target.addresses.first(), 22, targetFullName())
            context = new DryDeployContext(project, target, target.addresses.first(), log, target.directory)
            if (!EmbeddedTools.isInstantDryRun(project)) {
                log.log("-> Simulating timeout delay ${target.timeout}s (disable with -Pdeploy-dry-instant)")
                // Worker API allows parallel execution. Timeout delay is good for testing parallel execution of
                // larger projects
                workerExecutor.submit(SimulatedTimeoutWorker, ({ WorkerConfiguration config ->
                    config.isolationMode = IsolationMode.NONE
                    config.params target.timeout
                }) as Action)
            }
        } else {
            def password = target.password ?: ""
            if (target.promptPassword) {
                def tpassword = EmbeddedTools.promptPassword(target.user)
                if (tpassword != null) password = tpassword
            }

            // We only have to prompt once
            target.password = password
            target.promptPassword = false

            log.log("Discovering Target ${target.name}")

            // Assertions
            assert target.user != null
            assert target.timeout > 0

            def index = targetStorage.put(target)
            target.addresses.each { String addr ->
                // Submit some Workers on the Worker API to test addresses. This allows the task to run in parallel
                workerExecutor.submit(DiscoverTargetWorker, ({ WorkerConfiguration config ->
                    config.isolationMode = IsolationMode.NONE
                    config.params addr, targetFullName(), index
                } as Action))
            }
            // Wait for all workers to complete
            workerExecutor.await()

            if (addressStorage.findAll { it.target.equals(targetFullName()) }.empty) {
                if (target.failOnMissing)
                    throw new TargetNotFoundException("Target ${target.name} could not be located! Failing as ${target.name}.failOnMissing is true.")
                else
                    log.log("Target ${target.name} could not be located! Skipping target as ${target.name}.failOnMissing is false.")
            } else {
                log.log("Using address ${activeAddress().address}:${activeAddress().port} for target ${target.name}")

                session = new SshSessionController(activeAddress().address, activeAddress().port, target.user, target.password, target.timeout)
                context = new DefaultDeployContext(project, target, activeAddress().address, log, session, target.directory)
            }
        }
    }

    @Internal
    boolean isTargetActive() {
        return !addressStorage.findAll { it.target.equals(targetFullName()) }.empty
    }

    AddressStorage activeAddress() {
        return addressStorage.findAll { it.target.equals(targetFullName()) }.sort { AddressStorage addrStor -> target.addresses.indexOf(addrStor.address) }.first()        // Order based on what order addresses registered
    }

    static class DiscoverTargetWorker implements Runnable {
        String host
        String fullTargetName
        int index

        @Inject
        DiscoverTargetWorker(String host, String fullTargetName, Integer index) {
            this.index = index
            this.host = host
            this.fullTargetName = fullTargetName
        }

        @Override
        void run() {
            def log = LoggerFactory.getLogger('embedded_tools')
            try {
                def target = targetStorage.get(index)
                def thread = new Thread({
                    try {
                        log.debug("Trying address ${host}")
                        def splitHost = host.split(":")
                        def hostname = splitHost[0]
                        def port = splitHost.length > 1 ? Integer.parseInt(splitHost[1]) : 22

                        String originalHost = host
                        boolean updated = false
                        for (InetAddress addr : InetAddress.getAllByName(hostname)) {
                            if (!addr.isMulticastAddress() && (target.ipv6 || addr instanceof Inet4Address)) {
                                log.info("Resolved ${hostname} -> ${addr.getHostAddress()}")
                                host = addr.getHostAddress()
                                updated = true
                                break;
                            }
                        }
                        if (!updated) {
                            log.debug("No resolution, using raw host address ${host}")
                        }
                        log.debug("Using HOST=${host} PORT=${port}")
                        def session = new SshSessionController(host, port, target.user, target.password, target.timeout)
                        log.info("Found ${host}! (${originalHost})")
                        session.disconnect()
                        addressStorage.push(new AddressStorage(host, port, fullTargetName))
                        target.latch.countDown()
                    } catch (InterruptedException e) {
                        log.debug("${host} discovery thread interrupted")
                        Thread.currentThread().interrupt()
                    } catch (Exception e) {
                        def s = new StringWriter()
                        def pw = new PrintWriter(s)
                        e.printStackTrace(pw)
                        log.debug("[i] Could not reach ${host}...")
                        log.debug(s.toString())
                    }
                })
                thread.start()
                if (target.discoverInstant) {
                    target.latch.await(target.timeout*1000 + 500, TimeUnit.MILLISECONDS) // Add 500 to account for Thread spinup
                    thread.interrupt()
                    log.debug("Interrupting discovery thread ${host}...")
                } else {
                    thread.join()
                }
            } catch (Exception e) {
                def s = new StringWriter()
                def pw = new PrintWriter(s)
                e.printStackTrace(pw)
                log.debug("Could not reach ${host}...")
                log.debug(s.toString())
            }
        }
    }

    static class SimulatedTimeoutWorker implements Runnable {
        int timeout

        @Inject
        SimulatedTimeoutWorker(Integer timeout) {
            this.timeout = timeout
        }

        @Override
        void run() {
            Thread.sleep(timeout * 1000)
        }
    }

    class TargetNotFoundException extends RuntimeException {
        TargetNotFoundException(String msg) {
            super(msg)
        }
    }
}
