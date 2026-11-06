package cn.ericcraft.elytraPlus.fabric.runtime;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Installs the native adapter selected by Fabric Loader. */
public final class RuntimeInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        cn.ericcraft.elytraPlus.fabric.FabricServices.install(new cn.ericcraft.elytraPlus.fabric.runtime.NativeAccessImpl());
        marker("runtime-initialized");
    }

    public static void marker(String stage) {
        if (!Boolean.getBoolean("elytraplus.diagnostics")) { return; }
        String game = FabricLoader.getInstance().getModContainer("elytraplus_runtime").get()
                .getMetadata().getCustomValue("elytraplus:game").getAsString();
        System.out.println("ELYTRAPLUS_ARCHITECTURE game=" + game + " stage=" + stage);
    }
}
