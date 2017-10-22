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
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory

import javax.inject.Inject

@CompileStatic
class TargetDiscoveryTask extends DefaultTask {

    final WorkerExecutor workerExecutor
    static WorkerStorage<RemoteTarget>  targetStorage  = WorkerStorage.obtain()
    static WorkerStorage<String>        addressStorage = WorkerStorage.obtain()

    DeployLogger            log
    SshSessionController    session
    DeployContext           context

    @Input
    RemoteTarget target

    @Inject
    TargetDiscoveryTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void discoverTarget() {
        // Ask for password if needed
        log = new DeployLogger(0)

        if (EmbeddedTools.isDryRun(project)) {
            log.log("Dry Run! Using ${target.addresses.first()} for target ${target.name}")
            addressStorage << target.addresses.first()
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

            // Just in case
            addressStorage.clear()

            def index = targetStorage.put(target)
            target.addresses.each { String addr ->
                // Submit some Workers on the Worker API to test addresses. This allows the task to run in parallel
                workerExecutor.submit(DiscoverTargetWorker, ({ WorkerConfiguration config ->
                    config.isolationMode = IsolationMode.NONE
                    config.params addr, index
                } as Action))
            }
            // Wait for all workers to complete
            workerExecutor.await()

            if (addressStorage.empty) {
                if (target.failOnMissing)
                    throw new TargetNotFoundException("Target ${target.name} could not be located! Failing as ${target.name}.failOnMissing is true.")
                else
                    log.log("Target ${target.name} could not be located! Skipping target as ${target.name}.failOnMissing is false.")
            } else {
                log.log("Using address ${activeAddress()} for target ${target.name}")

                session = new SshSessionController(activeAddress(), target.user, target.password, target.timeout)
                context = new DefaultDeployContext(project, target, activeAddress(), log, session, target.directory)
            }
        }
    }

    boolean isTargetActive() {
        return !addressStorage.empty
    }

    String activeAddress() {
        return addressStorage.sort { String addr -> target.addresses.indexOf(addr) }.first()        // Order based on what order addresses registered
    }

    static class DiscoverTargetWorker implements Runnable {
        String host
        int index

        @Inject
        DiscoverTargetWorker(String host, Integer index) {
            this.index = index
            this.host = host
        }

        @Override
        void run() {
            try {
                def target = targetStorage.get(index)
                def session = new SshSessionController(host, target.user, target.password, target.timeout)
                println "Found ${host}!"
                session.disconnect()
                addressStorage.push(host)
            } catch (Exception e) {
                def s = new StringWriter()
                def pw = new PrintWriter(s)
                e.printStackTrace(pw)
                def log = LoggerFactory.getLogger('embedded_tools')
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
