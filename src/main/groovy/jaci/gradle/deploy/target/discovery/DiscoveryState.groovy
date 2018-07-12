package jaci.gradle.deploy.target.discovery

enum DiscoveryState {
    // STARTED and RESOLVED have the same priority since IP addresses will always pass resolution,
    // but hostnames won't. So in the case no addresses can be reached, we want to sort based on
    // the location order.
    NOT_STARTED("not started", 0),
    STARTED("failed resolution", 10),
    RESOLVED("resolved but not connected", 10),
    CONNECTED("connected", 20)

    String stateLocalized
    int priority
    DiscoveryState(String local, int pri) {
        this.stateLocalized = local
        this.priority = pri
    }
}