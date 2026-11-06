package cn.ericcraft.elytraPlus.fabric;

import cn.ericcraft.elytraPlus.core.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/** State is owned by a server, never by the client world or a player UUID globally. */
public final class FabricServices {
    private static final Logger LOGGER = Logger.getLogger("ElytraPlus");
    private static final Map<Object, State> SERVERS = Collections.synchronizedMap(new IdentityHashMap<>());
    private static NativeAccess access;
    private static PermissionBridge permissions;
    private FabricServices() { }
    public static void install(NativeAccess selected) {
        if (access != null) throw new IllegalStateException("Multiple ElytraPlus runtimes loaded");
        access = selected;
        permissions = PermissionBridge.discover(FabricServices.class.getClassLoader());
    }
    public static boolean permission(Object source, String node, boolean fallback) { return permissions.check(source, node, fallback); }
    public static void start(Object server) { start(server, FabricLoader.getInstance().getConfigDir().resolve("elytraplus")); }
    public static void start(Object server, Path directory) {
        if (SERVERS.containsKey(server)) throw new IllegalStateException("ElytraPlus already started for server");
        FabricConfiguration config = FabricConfiguration.load(directory);
        SERVERS.put(server, new State(server, config));
        if (config.settings.metricsEnabled) LOGGER.info("bstats.enabled has no effect on Fabric; no metrics are uploaded.");
    }
    public static void stop(Object server) {
        State state = SERVERS.remove(server);
        if (state != null) state.engine.shutdown();
    }
    public static void tick(Object server) {
        State state = SERVERS.get(server);
        if (state != null && ++state.ticks >= state.config.settings.interval) { state.ticks = 0; state.engine.tick(); }
    }
    public static int command(Object source, boolean extraArguments) {
        State state = SERVERS.get(access.commandServer(source));
        if (state == null) return 0;
        Object actor = access.commandPlayer(source);
        if (actor == null) { access.reply(source, state.config.messages.text("only-player")); return 0; }
        FlightPlayer player = access.wrap(actor);
        if (extraArguments) { state.config.messages.send(player, "usage"); return 0; }
        state.engine.toggle(player);
        if (state.engine.hasSession(player.id())) state.worlds.put(player.id(), player.world());
        else state.worlds.remove(player.id());
        return 1;
    }
    public static boolean flightRequest(Object actor) {
        State state = state(actor);
        if (state == null) return true;
        FlightPlayer player = access.wrap(actor);
        return !state.engine.hasSession(player.id()) || state.engine.validate(player);
    }
    public static void playerTick(Object actor) {
        State state = state(actor);
        if (state == null) return;
        FlightPlayer player = access.wrap(actor);
        if (!state.engine.hasSession(player.id())) { state.worlds.remove(player.id()); return; }
        if (player.nativeFlight() || player.dead() || !player.world().equals(state.worlds.get(player.id()))) {
            state.engine.validate(player);
            if (state.engine.hasSession(player.id())) state.worlds.put(player.id(), player.world());
            else state.worlds.remove(player.id());
        }
    }
    public static void death(Object actor) {
        State state = state(actor);
        if (state == null) return;
        FlightPlayer player = access.wrap(actor);
        if (state.engine.stop(player, null)) state.respawn.add(player.id());
        state.worlds.remove(player.id());
    }
    public static void respawn(Object actor) {
        State state = state(actor);
        if (state == null) return;
        FlightPlayer player = access.wrap(actor);
        boolean stopped = state.engine.stop(player, null);
        if (state.respawn.remove(player.id()) || stopped) state.config.messages.send(player, "respawn-disabled");
        state.worlds.remove(player.id());
    }
    public static void quit(Object actor) {
        State state = state(actor);
        if (state == null) return;
        FlightPlayer player = access.wrap(actor);
        state.engine.stop(player, null); state.respawn.remove(player.id()); state.worlds.remove(player.id());
    }
    private static State state(Object actor) { return SERVERS.get(access.playerServer(actor)); }
    private static final class State {
        final FabricConfiguration config;
        final FlightEngine engine;
        final Set<UUID> respawn = new HashSet<>();
        final Map<UUID, String> worlds = new HashMap<>();
        long ticks;
        State(Object server, FabricConfiguration config) {
            this.config = config;
            engine = new FlightEngine(config.settings, config.messages, id -> access.find(server, id),
                    () -> ThreadLocalRandom.current().nextDouble(), LOGGER);
        }
    }
}
