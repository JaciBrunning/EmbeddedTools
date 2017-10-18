package jaci.gradle.deploy.tasks

import groovy.transform.CompileStatic
import jaci.gradle.EmbeddedTools
import jaci.gradle.WorkerStorage
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.transport.SshSessionController
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

@CompileStatic
class TargetDiscoveryTask extends DefaultTask {

    final WorkerExecutor workerExecutor
    static WorkerStorage<RemoteTarget>  targetStorage = WorkerStorage.obtain()
    static WorkerStorage<String>        addressStorage = WorkerStorage.obtain()

    @Input
    RemoteTarget target

    @Inject
    TargetDiscoveryTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void discoverTarget() {
        if (isTargetActive()) {
            println "Target ${target.name} already found at ${activeAddress()}! Not checking again..."
            // StopExecutionException doesn't halt the build, just stops this task from executing further, analogous
            // to an early return.
            throw new StopExecutionException()
        }
        // Ask for password if needed
        def password = target.password ?: ""
        if (target.promptPassword) {
            def tpassword = EmbeddedTools.promptPassword(target.user)
            if (tpassword != null) password = tpassword
        }

        // We only have to prompt once
        target.password = password
        target.promptPassword = false

        // TODO Better Logging
        println("Discovering Target ${target.name}")

        // Assertions
        assert target.user != null
        assert target.timeout > 0

        // Just in case
        addressStorage.clear()

        def index = targetStorage.put(target)
        target.addresses.each { String addr ->
            // Submit some Workers on the Worker API to test addresses. This allows the task to run in parallel
            workerExecutor.submit(DiscoverSingleTarget, ({ WorkerConfiguration config ->
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
                println "Target ${target.name} could not be located! Skipping target as ${target.name}.failOnMissing is false."
        } else {
            println "Using address ${activeAddress()} for target ${target.name}"
        }
    }

    boolean isTargetActive() {
        return !addressStorage.empty
    }

    String activeAddress() {
        return addressStorage.first()       // First address to respond is usually the fastest address
    }

    static class DiscoverSingleTarget implements Runnable {
        String host
        int index

        @Inject
        DiscoverSingleTarget(String host, Integer index) {
            this.index = index
            this.host = host
        }

        @Override
        void run() {
            try {
                def target = targetStorage.get(index)
                def session = new SshSessionController(host, target.user, target.password, target.timeout)
                println "Found ${host}!"
                addressStorage.push(host)
            } catch (all) { }
        }
    }

    class TargetNotFoundException extends RuntimeException {
        TargetNotFoundException(String msg) {
            super(msg)
        }
    }
}
