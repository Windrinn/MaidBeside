package com.kamikaguya.maid_beside.compat.superbwarfare.task;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.kamikaguya.maid_beside.compat.superbwarfare.handler.VehicleHandler;
import com.kamikaguya.maid_beside.config.MaidConfig;
import com.kamikaguya.maid_beside.main.MaidBeside;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.OptionalBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MaidRangedDriveToTarget {
    public static BehaviorControl<EntityMaid> create() {
        return BehaviorBuilder.create(maidInstance -> maidInstance.group(
                        maidInstance.registered(MemoryModuleType.WALK_TARGET), // 使用 WALK_TARGET 获取女仆的移动意图
                        maidInstance.present(MemoryModuleType.ATTACK_TARGET),
                        maidInstance.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES))
                .apply(maidInstance, (walkTargetMemory, attackTargetMemory, livingEntitiesMemory) ->
                        driveToTarget(maidInstance, walkTargetMemory, attackTargetMemory, livingEntitiesMemory)));
    }

    @NotNull
    private static Trigger<EntityMaid> driveToTarget(
            BehaviorBuilder.Instance<EntityMaid> maidInstance,
            MemoryAccessor<OptionalBox.Mu, WalkTarget> walkTargetMemory, // 接收寻路目标
            MemoryAccessor<IdF.Mu, LivingEntity> attackTargetMemory,
            MemoryAccessor<OptionalBox.Mu, NearestVisibleLivingEntities> livingEntitiesMemory) {

        return (level, maid, gameTime) -> {
            // 检查女仆是否在驾驶载具
            if (!VehicleHandler.shouldControlVehicle(maid)) {
                return false;
            }

            Entity vehicle = maid.getVehicle();
            if (!(vehicle instanceof MobileVehicleEntity mobileVehicle)) {
                return false;
            }

            if (maid.getBrain().hasMemoryValue(TaskDriveVehicle.PING_TARGET())) {
                return false;
            }

            // 获取攻击目标（用于决定是否追击）
            LivingEntity target = maidInstance.get(attackTargetMemory);
            if (!target.isAlive()) {
                return false;
            }

            Vec3 ownerPos = maid.getOwner().position();
            Vec3 toOwner = ownerPos.subtract(mobileVehicle.position()).multiply(1, 0, 1);
            double distanceToOwner = toOwner.length();
            if (distanceToOwner > MaidConfig.MAID_SEARCH_DISTANCE.get()) {
                VehicleHandler.controlVehicleToEntityTarget(maid, mobileVehicle, ownerPos);
            }

            // 检查目标是否可见
            Optional<NearestVisibleLivingEntities> nearestVisible = maidInstance.tryGet(livingEntitiesMemory);
            if (nearestVisible.isPresent() && !nearestVisible.get().contains(target)) {
                // 目标不可见，但可以尝试记忆中的最后位置
                Optional<WalkTarget> lastWalkTarget = maidInstance.tryGet(walkTargetMemory);
                if (lastWalkTarget.isEmpty()) {
                    return false; // 没有记忆位置，放弃
                }
                // 使用最后已知位置
                Vec3 targetPos = lastWalkTarget.get().getTarget().currentPosition();
                VehicleHandler.controlVehicleToEntityTarget(maid, mobileVehicle, targetPos);
                return true;
            }

            // 获取女仆AI计算出的下一个移动目标位置
            Optional<WalkTarget> walkTarget = maidInstance.tryGet(walkTargetMemory);
            if (walkTarget.isEmpty()) {
                return false;
            }

            Vec3 targetPos = walkTarget.get().getTarget().currentPosition();

            // 将移动意图委托给控制层处理
            VehicleHandler.controlVehicleToEntityTarget(maid, mobileVehicle, targetPos);

            if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
                MaidBeside.LOGGER.debug("MaidRangedDriveToTarget - MaidPos: {}, TargetPos: {}, Distance: {}", maid.position(), targetPos, maid.position().distanceTo(targetPos));
            }
            return true;
        };
    }
}