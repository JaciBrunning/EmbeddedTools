package jaci.gradle.deploy.discovery

enum DiscoveryState {
    // STARTED and RESOLVED have the same priority since IP addresses will always pass resolution,
    // but hostnames won't. So in the case no addresses can be reached, we want to sort based on
    // the location order.
    STARTED("failed resolution", 0),
    RESOLVED("resolved but not connected", 0),
    CONNECTED("connected", 1)

    String stateLocalized
    int priority
    DiscoveryState(String local, int pri) {
        this.stateLocalized = local
        this.priority = pri
    }
}