package com.kamikaguya.maid_beside.main;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.kamikaguya.maid_beside.compat.pingwheel.handler.PingHandler;
import com.kamikaguya.maid_beside.compat.superbwarfare.ai.brain.LittleMaidImpl;
import com.kamikaguya.maid_beside.compat.superbwarfare.handler.VehicleHandler;
import com.kamikaguya.maid_beside.config.GeneralConfig;
import com.kamikaguya.maid_beside.registry.MaidBesideRegistry;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("maid_beside")
public class MaidBeside {

    public static final String MODID = "maid_beside";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidBeside() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MaidBesideRegistry.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.init());
        bus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);

        // ILittleMaid实现到 TLM 的扩展列表
        TouhouLittleMaid.EXTENSIONS.add(new LittleMaidImpl());

        if (ModList.get().isLoaded("superbwarfare")) {
            MinecraftForge.EVENT_BUS.register(VehicleHandler.class);
        }

        if (ModList.get().isLoaded("pingwheel")) {
            MinecraftForge.EVENT_BUS.register(PingHandler.class);
            // PingWheelIntegration.setup();
        }
    }

    public void setup(FMLCommonSetupEvent event) {}

    public void doClientStuff(final FMLClientSetupEvent event) {}

    public static boolean isPhysicalClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public void setupClient(final FMLClientSetupEvent event) {}

    public void setupComplete(final FMLLoadCompleteEvent event) {}
}