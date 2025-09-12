package com.kamikaguya.maid_beside.compat.superbwarfare.task;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.kamikaguya.maid_beside.compat.superbwarfare.handler.VehicleHandler;
import com.kamikaguya.maid_beside.config.MaidConfig;
import com.kamikaguya.maid_beside.main.MaidBeside;
import com.mojang.datafixers.kinds.OptionalBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StopVehicleIfNoTarget {
    private static final Map<UUID, Integer> noTargetCounters = new HashMap<>();

    public static BehaviorControl<EntityMaid> create() {
        return BehaviorBuilder.create(maidInstance -> maidInstance.group(
                        maidInstance.registered(MemoryModuleType.ATTACK_TARGET))
                .apply(maidInstance, (attackTargetMemory) -> stopVehicle(maidInstance, attackTargetMemory)));
    }

    @NotNull
    private static Trigger<EntityMaid> stopVehicle(
            BehaviorBuilder.Instance<EntityMaid> maidInstance,
            MemoryAccessor<OptionalBox.Mu, LivingEntity> attackTargetMemory) {

        return (level, maid, gameTime) -> {
            Optional<LivingEntity> targetOpt = maidInstance.tryGet(attackTargetMemory);
            UUID maidUUID = maid.getUUID();

            if (targetOpt.isEmpty() || !targetOpt.get().isAlive()) {
                int count = noTargetCounters.getOrDefault(maidUUID, 0) + 1;
                noTargetCounters.put(maidUUID, count);

                // 连续3帧没有目标才停止
                if (count >= 3) {
                    Entity vehicle = maid.getVehicle();
                    if (vehicle instanceof MobileVehicleEntity mobileVehicle) {
                        VehicleHandler.stopVehicle(mobileVehicle);
                        VehicleHandler.cleanupVehicleState(mobileVehicle.getUUID());
                        if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
                            MaidBeside.LOGGER.debug("StopVehicleIfNoTarget - Vehicle stopped");
                        }
                    }
                    noTargetCounters.put(maidUUID, 0);
                    return true;
                }
            } else {
                noTargetCounters.put(maidUUID, 0); // 重置计数器
            }

            return false;
        };
    }
}