package cn.ericcraft.elytraPlus.fabric;

import cn.ericcraft.elytraPlus.core.FlightPlayer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FabricServicesTest {
    private static NativeAccess access;
    @TempDir Path directory;
    private Object server, actor, source;
    private FlightPlayer player;
    private AtomicBoolean allow, flying;
    @BeforeAll static void install() { access = mock(NativeAccess.class); FabricServices.install(access); }
    @BeforeEach void setup() {
        server = new Object(); actor = new Object(); source = new Object();
        player = mock(FlightPlayer.class); allow = new AtomicBoolean(); flying = new AtomicBoolean();
        when(access.playerServer(actor)).thenReturn(server);
        when(access.commandServer(source)).thenReturn(server);
        when(access.commandPlayer(source)).thenReturn(actor);
        when(access.wrap(actor)).thenReturn(player);
        UUID id = UUID.randomUUID(); when(player.id()).thenReturn(id);
        when(access.find(server, id)).thenReturn(player);
        when(player.online()).thenReturn(true); when(player.permission("elytraplus.use")).thenReturn(true);
        when(player.world()).thenReturn("minecraft:overworld"); when(player.wearingElytra()).thenReturn(true); when(player.usableElytra()).thenReturn(true);
        when(player.allowFlight()).thenAnswer(i -> allow.get()); when(player.flying()).thenAnswer(i -> flying.get());
        doAnswer(i -> { allow.set(i.getArgument(0)); return null; }).when(player).allowFlight(anyBoolean());
        doAnswer(i -> { flying.set(i.getArgument(0)); return null; }).when(player).flying(anyBoolean());
        FabricServices.start(server, directory);
    }
    @AfterEach void stop() { FabricServices.stop(server); }
    @Test void enablingDoesNotForceTakeoffAndIntervalUsesServerTicks() {
        FabricServices.command(source, false); assertTrue(allow.get()); assertFalse(flying.get());
        flying.set(true);
        for (int i = 0; i < 19; i++) FabricServices.tick(server);
        verify(player, never()).damageOne(); FabricServices.tick(server); verify(player).damageOne();
    }
    @Test void stopRevokesOwnedFlightAndClearsTimer() {
        FabricServices.command(source, false); FabricServices.stop(server); assertFalse(allow.get());
        FabricServices.stop(server); FabricServices.tick(server); assertEquals(0, FabricServices.command(source, false));
    }
    @Test void deathRespawnAndReconnectRequireNewCommand() {
        FabricServices.command(source, false); FabricServices.death(actor); assertFalse(allow.get());
        FabricServices.respawn(actor); verify(player).message(contains("重生"));
        FabricServices.command(source, false); FabricServices.quit(actor); assertFalse(allow.get());
        FabricServices.respawn(actor); assertFalse(allow.get());
    }
    @Test void invalidTakeoffClosesTheSession() {
        FabricServices.command(source, false); when(player.usableElytra()).thenReturn(false);
        assertFalse(FabricServices.flightRequest(actor)); assertFalse(allow.get());
    }
    @Test void creativeModeKeepsNativeAbilities() {
        FabricServices.command(source, false); when(player.nativeFlight()).thenReturn(true); flying.set(true);
        FabricServices.playerTick(actor); FabricServices.stop(server); assertTrue(allow.get()); assertTrue(flying.get());
    }
    @Test void consoleAndExtraArgumentsNeverCreateFlight() {
        FabricServices.command(source, true); assertFalse(allow.get());
        when(access.commandPlayer(source)).thenReturn(null); FabricServices.command(source, false);
        verify(access).reply(eq(source), anyString()); assertFalse(allow.get());
    }
    @Test void separateServerWithSamePlayerIdDoesNotShareSessionOrTimer() {
        Object second = new Object(); FlightPlayer other = mock(FlightPlayer.class);
        UUID id = player.id();
        when(access.find(second, id)).thenReturn(other); when(other.id()).thenReturn(id);
        FabricServices.start(second, directory);
        try {
            FabricServices.command(source, false); FabricServices.tick(second); FabricServices.stop(second);
            assertTrue(allow.get()); verify(other, never()).allowFlight(anyBoolean());
        } finally { FabricServices.stop(second); }
    }
}
