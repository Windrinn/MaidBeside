package com.kamikaguya.maid_beside.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.kamikaguya.maid_beside.main.MaidBeside;
import com.kamikaguya.maid_beside.compat.superbwarfare.handler.VehicleHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber(modid = MaidBeside.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MaidBesideHandler {
    // 存储需要骑乘的女仆和载具信息
    private static final Map<UUID, DelayedRideInfo> DELAYED_RIDES = new HashMap<>();
    // 存储玩家与载具的关联
    private static final Map<UUID, Entity> PLAYER_VEHICLES = new HashMap<>();
    // 存储需要检查的玩家骑乘状态
    private static final Map<UUID, Integer> PLAYER_DISMOUNT_CHECK = new HashMap<>();

    // 延迟执行的信息记录类
    private record DelayedRideInfo(Entity maid, Entity vehicle, long scheduledTick) {
    }

    @SubscribeEvent
    public static void onPlayerMountVehicle(EntityMountEvent event) {
        // 只在服务端处理且是骑乘事件
        if (!event.isCanceled() && event.isMounting() && !event.getEntity().level().isClientSide()) {
            if (event.getEntity() instanceof ServerPlayer player) {
                Entity vehicle = event.getEntityBeingMounted();

                // 确保载具存在
                if (vehicle != null && !VehicleHandler.isLowHealth(vehicle)) {
                    // 获取玩家附近的所有女仆
                    List<Entity> nearbyEntities = player.level().getEntities(player,
                            player.getBoundingBox().inflate(10.0D),
                            entity -> entity instanceof EntityMaid
                    );

                    // 为每个女仆安排延迟骑乘
                    for (Entity entity : nearbyEntities) {
                        EntityMaid maid = (EntityMaid) entity;

                        if (maid.isAlive() &&
                                maid.getOwnerUUID() != null &&
                                maid.getOwnerUUID().equals(player.getUUID()) &&
                                !maid.isPassenger()) {
                            // 稍等片刻再让女仆骑乘，确保玩家已经完成骑乘
                            // 计算延迟执行的时间点（2刻后）
                            long scheduledTick = player.level().getGameTime() + 2;
                            DELAYED_RIDES.put(maid.getUUID(), new DelayedRideInfo(maid, vehicle, scheduledTick));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 在服务器刻结束时检查延迟骑乘
            long currentTick = event.getServer().overworld().getGameTime();

            Iterator<Map.Entry<UUID, DelayedRideInfo>> iterator = DELAYED_RIDES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, DelayedRideInfo> entry = iterator.next();
                DelayedRideInfo rideInfo = entry.getValue();

                if (currentTick >= rideInfo.scheduledTick) {
                    // 执行延迟骑乘
                    if (rideInfo.maid.isAlive() && rideInfo.vehicle.isAlive() &&
                            rideInfo.vehicle.getPassengers().size() < getMaxPassengersForEntity(rideInfo.vehicle)) {

                        // 只要玩家在载具上即可
                        boolean playerIsRiding = rideInfo.vehicle.getPassengers().stream()
                                .anyMatch(passenger -> passenger instanceof ServerPlayer);

                        if (playerIsRiding) {
                            // 让女仆骑乘，但不强制
                            rideInfo.maid.startRiding(rideInfo.vehicle, false);
                        }
                    }

                    // 移除已处理的任务
                    iterator.remove();
                }
            }
        }
    }

    public static int getMaxPassengersForEntity(Entity entity) {
        // 安全地获取实体的最大乘客数
        try {
            // 检查常见原版实体的乘客数量
            if (entity instanceof Boat) return 1;
            if (entity instanceof Minecart) return 1;
            if (entity instanceof AbstractHorse) return 2;

            // 尝试使用反射获取（如果有getMaxPassengers方法）
            try {
                Method getMaxPassengersMethod = Entity.class.getMethod("getMaxPassengers");
                return (int) getMaxPassengersMethod.invoke(entity);
            } catch (NoSuchMethodException e) {
                // 方法不存在，使用默认值
                return VehicleHandler.getMaxPassengers(entity);
            }
        } catch (Exception e) {
            return 2; // 默认安全值
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide() &&
                event.player instanceof ServerPlayer player) {

            Entity currentVehicle = player.getVehicle();
            UUID playerUUID = player.getUUID();
            Entity previousVehicle = PLAYER_VEHICLES.get(playerUUID);

            // 检查玩家是否在骑乘
            if (previousVehicle != null && currentVehicle == null) {
                // 玩家已取消骑乘，让女仆也取消骑乘
                dismissMaidsFromVehicle(player, previousVehicle);
            }

            // 检查载具血量是否健康
            if (previousVehicle != null && VehicleHandler.isLowHealth(currentVehicle)) {
                // 载具低血量时跳车
                dismissMaidsFromVehicle(player, previousVehicle);
            }

            // 更新玩家载具状态
            PLAYER_VEHICLES.put(playerUUID, currentVehicle);

            // 处理延迟取消骑乘检查（防止瞬时状态误判）
            if (PLAYER_DISMOUNT_CHECK.containsKey(playerUUID)) {
                int checkCount = PLAYER_DISMOUNT_CHECK.get(playerUUID);
                if (checkCount > 0) {
                    PLAYER_DISMOUNT_CHECK.put(playerUUID, checkCount - 1);
                } else {
                    Entity storedVehicle = PLAYER_VEHICLES.get(playerUUID);
                    if (storedVehicle != null && player.getVehicle() == null) {
                        dismissMaidsFromVehicle(player, storedVehicle);
                    }
                    PLAYER_DISMOUNT_CHECK.remove(playerUUID);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDismountVehicle(EntityMountEvent event) {
        if (!event.isMounting() && !event.getEntity().level().isClientSide() &&
                event.getEntity() instanceof ServerPlayer player) {

            // 玩家骑乘事件
            Entity vehicle = event.getEntityBeingMounted();

            if (vehicle != null) {
                // 延迟检查
                PLAYER_DISMOUNT_CHECK.put(player.getUUID(), 2); // 2 刻后检查

                // 让所有女仆取消骑乘
                dismissMaidsFromVehicle(player, vehicle);
            }
        }
    }

    @SubscribeEvent
    public static void onMaidTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof EntityMaid maid && isMaidDriver(maid, maid.getVehicle())) {

            Entity currentVehicle = maid.getVehicle();

            // 检查载具血量是否健康
            if (VehicleHandler.isLowHealth(currentVehicle)) {
                // 载具低血量时跳车
                dismissMaidFromVehicle(maid, currentVehicle);
            }
        }
    }

    public static boolean isMaidDriver(EntityMaid maid, Entity vehicle) {
        if (vehicle == maid.getVehicle()) {
            return VehicleHandler.isDriver(maid, vehicle);
        } else
            return false;
    }

    /**
     * 让所有女仆从载具下车
     */
    private static void dismissMaidsFromVehicle(ServerPlayer player, Entity vehicle) {
        if (vehicle == null) return;

        // 查找所有骑乘在该载具上的女仆
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof EntityMaid maid &&
                    maid.getOwnerUUID() != null &&
                    maid.getOwnerUUID().equals(player.getUUID()) &&
                    !isMaidDriver(maid, vehicle)) {

                // 让女仆取消骑乘
                maid.stopRiding();
            }
        }
    }

    private static void dismissMaidFromVehicle(EntityMaid maid, Entity vehicle) {
        if (vehicle == null) return;

        // 查找所有骑乘在该载具上的女仆
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof EntityMaid) {
                // 让女仆取消骑乘
                maid.stopRiding();
            }
        }
    }

    // 清理数据
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerUUID = event.getEntity().getUUID();
        PLAYER_VEHICLES.remove(playerUUID);
        PLAYER_DISMOUNT_CHECK.remove(playerUUID);
    }
}