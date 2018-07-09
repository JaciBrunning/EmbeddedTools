package jaci.gradle.deploy.discovery

enum DiscoveryState {
    STARTED("Started but not resolved", 0),
    RESOLVED("Resolved but not connected", 1),
    CONNECTED("Connected", 2)

    String stateLocalized
    int priority
    DiscoveryState(String local, int pri) {
        this.stateLocalized = local
        this.priority = pri
    }
}