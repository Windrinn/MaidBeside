package com.kamikaguya.maid_beside.compat.superbwarfare.task;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import com.kamikaguya.maid_beside.compat.superbwarfare.handler.VehicleHandler;
import com.kamikaguya.maid_beside.config.MaidConfig;
import com.kamikaguya.maid_beside.main.MaidBeside;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

public class MaidDriveToPingTarget extends Behavior<EntityMaid> {
    private int logCooldown = 0;

    public MaidDriveToPingTarget() {
        // 指定该行为依赖的记忆模块及其状态
        // 这里要求 PING_TARGET 记忆必须存在 (VALUE_PRESENT)
        super(ImmutableMap.of(
                TaskDriveVehicle.PING_TARGET(), MemoryStatus.VALUE_PRESENT
        ), 3600); // 3600 是该行为的最大持续时间（tick）
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid maid) {
        if (MaidConfig.MAID_BESIDE_DEBUG.get() && logCooldown <= 0) {
            MaidBeside.LOGGER.debug("MaidDriveToPingTarget checkExtraStartConditions for maid: {}", maid.getUUID());
            logCooldown = 20; // 每20 tick记录一次
        } else {
            logCooldown--;
        }

        if (!VehicleHandler.shouldControlVehicle(maid)) {
            return false;
        }

        Entity vehicle = maid.getVehicle();
        if (!(vehicle instanceof MobileVehicleEntity)) {
            return false;
        }

        return true;
    }

    @Override
    protected boolean canStillUse(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        // 检查行为是否应该继续执行
        // 条件通常与checkExtraStartConditions类似，并且还要检查记忆是否仍然存在
        boolean canStillUse = maid.getBrain().hasMemoryValue(TaskDriveVehicle.PING_TARGET()) && this.checkExtraStartConditions(worldIn, maid);
        if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
            MaidBeside.LOGGER.debug("MaidDriveToPingTarget canStillUse for maid {}: {}", maid.getUUID(), canStillUse);
        }
        return canStillUse;
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        // 当行为开始时执行一次的逻辑
        if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
            MaidBeside.LOGGER.debug("MaidDriveToPingTarget STARTED for maid: {}", maid.getUUID());
        }
    }

    @Override
    protected void tick(ServerLevel worldIn, EntityMaid maid, long gameTime) {
        // 获取目标位置
        BlockPos pingTarget = maid.getBrain().getMemory(TaskDriveVehicle.PING_TARGET()).get();
        Entity vehicle = maid.getVehicle();
        MobileVehicleEntity mobileVehicle = (MobileVehicleEntity) vehicle;

        Vec3 currentPos = mobileVehicle.position();
        double distanceSqr = currentPos.distanceToSqr(pingTarget.getCenter());

        if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
            MaidBeside.LOGGER.debug("Maid {} driving to ping target: {}, Distance: {}, Speed: {}", maid.getUUID(), pingTarget, Math.sqrt(distanceSqr), mobileVehicle.getDeltaMovement().length());
        }

        Vec3 ownerPos = maid.getOwner().position();
        Vec3 toOwner = ownerPos.subtract(vehicle.position()).multiply(1, 0, 1);
        double distanceToOwner = toOwner.length();
        if (distanceToOwner > MaidConfig.MAID_SEARCH_DISTANCE.get()) {
            maid.getBrain().eraseMemory(TaskDriveVehicle.PING_TARGET());

            VehicleHandler.controlVehicleToEntityTarget(maid, mobileVehicle, ownerPos);
        }

        // 目标位置（ping目标的位置）
        Vec3 targetPos = pingTarget.getCenter();

        // 控制载具向目标移动
        VehicleHandler.controlVehicleHybrid(maid, mobileVehicle, targetPos);

        // 如果已经很接近目标，清除ping目标并停止此行为
        double stoppingDistance = 13.0; // 增加停止距离
        if (MaidConfig.MAID_BESIDE_DEBUG.get() && distanceSqr < (stoppingDistance * stoppingDistance)) {
            MaidBeside.LOGGER.debug("Maid {} reached ping target (distance: {})",
                    maid.getUUID(), Math.sqrt(distanceSqr));
            maid.getBrain().eraseMemory(TaskDriveVehicle.PING_TARGET());

            // 确保载具完全停止
            VehicleHandler.stopVehicle(mobileVehicle);
        }
    }

    @Override
    protected void stop(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        // 当行为停止时执行的清理逻辑
        if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
            MaidBeside.LOGGER.debug("MaidDriveToPingTarget STOPPED for maid: {}", maid.getUUID());
        }
    }
}