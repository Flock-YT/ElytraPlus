package cn.ericcraft.elytraPlus.fabric.bootstrap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/** Java 8 bootstrap: deliberately contains no references to Minecraft classes. */
public final class ElytraPlusFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModContainer runtime = FabricLoader.getInstance().getModContainer("elytraplus_runtime")
                .orElseThrow(() -> new IllegalStateException("ElytraPlus runtime was not selected"));
        String selected = runtime.getMetadata().getCustomValue("elytraplus:game").getAsString();
        String actual = FabricLoader.getInstance().getModContainer("minecraft").get()
                .getMetadata().getVersion().getFriendlyString();
        if (!selected.equals(actual)) {
            throw new IllegalStateException("ElytraPlus runtime mismatch: " + selected + " / " + actual);
        }
        if (Boolean.getBoolean("elytraplus.diagnostics")) System.out.println("ELYTRAPLUS_ARCHITECTURE selected=" + selected
                + " environment=" + FabricLoader.getInstance().getEnvironmentType()
                + " java=" + System.getProperty("java.version"));
    }
}
