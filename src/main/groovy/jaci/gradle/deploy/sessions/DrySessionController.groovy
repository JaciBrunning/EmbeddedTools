package jaci.gradle.deploy.sessions

import groovy.transform.CompileStatic

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
    String execute(String command) {
        return ""
    }

    @Override
    void put(List<File> sources, List<String> destinations) { }

    @Override
    void put(File source, String dest) { }

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
