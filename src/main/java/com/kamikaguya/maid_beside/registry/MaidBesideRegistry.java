package com.kamikaguya.maid_beside.registry;

import com.kamikaguya.maid_beside.main.MaidBeside;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class MaidBesideRegistry {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, MaidBeside.MODID);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> PING_TARGET = MEMORY_MODULE_TYPES.register("ping_target", () -> new MemoryModuleType<>(Optional.empty()));
}