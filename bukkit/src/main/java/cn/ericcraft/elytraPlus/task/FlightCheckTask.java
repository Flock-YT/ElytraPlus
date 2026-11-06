package cn.ericcraft.elytraPlus.task;

import cn.ericcraft.elytraPlus.manager.FlightManager;

public final class FlightCheckTask implements Runnable {
    private final FlightManager flights;
    public FlightCheckTask(FlightManager flights) { this.flights = flights; }
    @Override public void run() { flights.tick(); }
}
