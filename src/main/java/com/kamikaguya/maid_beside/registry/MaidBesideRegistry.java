package com.kamikaguya.maid_beside.registry;

import com.kamikaguya.maid_beside.main.MaidBeside;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = MaidBeside.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MaidBesideRegistry {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = DeferredRegister.create(ForgeRegistries.Keys.MEMORY_MODULE_TYPES, MaidBeside.MODID);
    public static final RegistryObject<MemoryModuleType<BlockPos>> PING_TARGET = MEMORY_MODULE_TYPES.register("ping_target", () -> new MemoryModuleType<>(Optional.empty()));

    public static void register(IEventBus modEventBus) {
        MEMORY_MODULE_TYPES.register(modEventBus);
    }
}