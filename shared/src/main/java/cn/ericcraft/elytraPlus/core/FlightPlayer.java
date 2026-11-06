package cn.ericcraft.elytraPlus.core;

import java.util.UUID;

/** Live server-thread view; implementations must not retain stale equipment snapshots. */
public interface FlightPlayer {
    UUID id();
    boolean online();
    boolean dead();
    boolean nativeFlight();
    boolean allowFlight();
    boolean flying();
    void allowFlight(boolean allow);
    void flying(boolean flying);
    boolean permission(String node);
    String world();
    boolean wearingElytra();
    boolean usableElytra();
    boolean unbreakable();
    int unbreaking();
    void damageOne();
    void breakSound();
    void message(String text);
}
