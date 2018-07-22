package jaci.gradle.deploy.sessions

import groovy.transform.CompileStatic
import jaci.gradle.deploy.CommandDeployResult

@CompileStatic
class DrySessionController extends AbstractSessionController implements IPSessionController {
    DrySessionController() {
        super(1)
    }

    @Override
    void open() {
        getLogger().info("DrySessionController opening")
    }

    @Override
    CommandDeployResult execute(String command) {
        return new CommandDeployResult(command, "", 0)
    }

    @Override
    void put(Map<String, File> files) { }

    @Override
    String friendlyString() {
        return "DrySessionController"
    }

    @Override
    void close() throws IOException {
        getLogger().info("DrySessionController closing")
    }

    @Override
    String getHost() {
        return "dryhost"
    }

    @Override
    int getPort() {
        return 22
    }
}
