package com.kamikaguya.maid_beside.compat.pingwheel.handler;

import com.kamikaguya.maid_beside.compat.superbwarfare.task.TaskDriveVehicle;
import com.kamikaguya.maid_beside.config.MaidConfig;
import com.kamikaguya.maid_beside.handler.MaidBesideHandler;
import com.kamikaguya.maid_beside.main.MaidBeside;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PingHandler {
    public static void handlePingForMaids(ServerPlayer authorPlayer, Vec3 pingPos) {
        // 查找女仆并设置记忆目标
        int distance = MaidConfig.MAID_SEARCH_DISTANCE.get();
        List<Entity> nearbyEntities = authorPlayer.level().getEntities(authorPlayer,
                new AABB(
                        authorPlayer.getX() - distance, authorPlayer.getY() - 8, authorPlayer.getZ() - distance,
                        authorPlayer.getX() + distance, authorPlayer.getY() + 8, authorPlayer.getZ() + distance
                ),
                entity -> entity instanceof EntityMaid
        );

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof EntityMaid maid)) {
                return;
            }

            if (pingPos.distanceTo(maid.position()) > distance) {
                return;
            }

            // 增加更详细的条件检查日志，方便定位哪个条件不满足
            boolean isAlive = maid.isAlive();
            boolean hasOwner = maid.getOwnerUUID() != null;
            boolean isOwner = hasOwner && maid.getOwnerUUID().equals(authorPlayer.getUUID());
            boolean isDriver = MaidBesideHandler.isMaidDriver(maid, maid.getVehicle());

            if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
                MaidBeside.LOGGER.debug("Checking maid {} for ping response", maid.getUUID());
                MaidBeside.LOGGER.debug("Maid {} conditions: alive={}, hasOwner={}, isOwner={}, isDriver={}", maid.getUUID(), isAlive, hasOwner, isOwner, isDriver);
            }

            if (isAlive && isOwner && isDriver) {
                BlockPos targetPos = BlockPos.containing(pingPos);
                // 重要：检查记忆模块类型是否已正确注册，且不为null
                TaskDriveVehicle.PING_TARGET();// 只设置一次记忆，但带有有效期
                maid.getBrain().setMemoryWithExpiry(TaskDriveVehicle.PING_TARGET(), targetPos, 3600); // 3600 tick = 180秒
                if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
                    MaidBeside.LOGGER.debug("Successfully set ping target memory for maid {} at position {}", maid.getUUID(), targetPos);
                }
            }
        }
    }
}