package com.kamikaguya.maid_beside.main;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.kamikaguya.maid_beside.compat.pingwheel.handler.PingHandler;
import com.kamikaguya.maid_beside.compat.superbwarfare.ai.brain.LittleMaidImpl;
import com.kamikaguya.maid_beside.compat.superbwarfare.handler.VehicleHandler;
import com.kamikaguya.maid_beside.config.GeneralConfig;
import com.kamikaguya.maid_beside.registry.MaidBesideRegistry;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod("maid_beside")
public class MaidBeside {
    public static final String MODID = "maid_beside";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidBeside(IEventBus eventBus, ModContainer modContainer) {
        initRegister(eventBus);
        registerConfiguration(modContainer);
        eventBus.addListener(this::setup);

        // ILittleMaid实现到 TLM 的扩展列表
        TouhouLittleMaid.EXTENSIONS.add(new LittleMaidImpl());

        if (ModList.get().isLoaded("superbwarfare")) {
            NeoForge.EVENT_BUS.register(VehicleHandler.class);
        }

        if (ModList.get().isLoaded("pingwheel")) {
            NeoForge.EVENT_BUS.register(PingHandler.class);
            // PingWheelIntegration.setup();
        }
    }
    private static void initRegister(IEventBus eventBus) {
        MaidBesideRegistry.MEMORY_MODULE_TYPES.register(eventBus);
    }

    private static void registerConfiguration(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, GeneralConfig.init());
    }

    public void setup(FMLCommonSetupEvent event) {}

    public void doClientStuff(final FMLClientSetupEvent event) {}

    public static boolean isPhysicalClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public void setupClient(final FMLClientSetupEvent event) {}

    public void setupComplete(final FMLLoadCompleteEvent event) {}
}