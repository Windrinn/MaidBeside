package com.kamikaguya.maid_beside.main;

import com.kamikaguya.maid_beside.registry.MaidBesideRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("maid_beside")
public class MaidBeside {

    public static final String MODID = "maid_beside";

    public MaidBeside() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MaidBesideRegistry.register(bus);
        bus.addListener(this::setup);
        bus.addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public void setup(final FMLCommonSetupEvent event) {

    }

    public void doClientStuff(final FMLClientSetupEvent event) {}

    public static boolean isPhysicalClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public void setupClient(final FMLClientSetupEvent event) {}

    public void setupComplete(final FMLLoadCompleteEvent event) {}
}