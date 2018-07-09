package jaci.gradle.deploy.tasks

enum DiscoveryState {
    NOT_RESOLVED("Not Resolved", 0),
    RESOLVED("Not Connected", 1),
    CONNECTED("Connected but Invalid", 2),
    VERIFIED("Valid", 3);

    String stateLocalized
    int priority
    DiscoveryState(String local, int pri) {
        this.stateLocalized = local
        this.priority = pri
    }
}