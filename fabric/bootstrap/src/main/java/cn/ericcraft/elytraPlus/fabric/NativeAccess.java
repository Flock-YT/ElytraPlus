package cn.ericcraft.elytraPlus.fabric;

import cn.ericcraft.elytraPlus.core.FlightPlayer;
import java.util.UUID;

/** Only the exact-version runtime references Minecraft types. */
public interface NativeAccess {
    FlightPlayer wrap(Object player);
    Object playerServer(Object player);
    Object commandServer(Object source);
    Object commandPlayer(Object source);
    FlightPlayer find(Object server, UUID id);
    void reply(Object source, String message);
}
