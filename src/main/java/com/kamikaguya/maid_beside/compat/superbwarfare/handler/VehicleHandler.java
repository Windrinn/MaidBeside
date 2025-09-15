package com.kamikaguya.maid_beside.compat.superbwarfare.handler;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.kamikaguya.maid_beside.config.MaidConfig;
import com.kamikaguya.maid_beside.main.MaidBeside;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VehicleHandler {
    private static final Map<UUID, Double> lastTurnDecision = new HashMap<>();
    private static final Map<UUID, Integer> turnConsistencyCounter = new HashMap<>();
    private static final Map<UUID, Long> turnStartTime = new HashMap<>();
    private static final Map<UUID, Double> initialCross = new HashMap<>();
    private static final Map<UUID, Double> speedHistory = new HashMap<>();
    private static final Map<UUID, Integer> stuckCounter = new HashMap<>();
    private static final Map<UUID, Vec3> lastPositions = new HashMap<>();
    private static final Map<UUID, Double> distanceHistory = new HashMap<>();

    public VehicleHandler() {
    }

    public static int getMaxPassengers(Entity entity) {
        if (entity instanceof VehicleEntity vehicle) {
            return vehicle.getMaxPassengers();
        } else
            return 2;
    }

    public static boolean isLowHealth(Entity entity) {
        if (entity instanceof VehicleEntity vehicle) {
            return vehicle.getHealth() <= vehicle.getMaxHealth() * 0.1;
        } else
            return false;
    }

    public static boolean isDriver(EntityMaid maid, Entity entity) {
        if (entity instanceof MobileVehicleEntity mobileVehicle && mobileVehicle.getFirstPassenger() != null) {
            return mobileVehicle.getFirstPassenger().getUUID().equals(maid.getUUID());
        } else
            return false;
    }

    // 检查女仆是否应该控制载具
    public static boolean shouldControlVehicle(EntityMaid maid) {
        Entity vehicle = maid.getVehicle();
        if (vehicle instanceof MobileVehicleEntity mobileVehicle) {
            return isDriver(maid, mobileVehicle);
        } else
            return false;
    }

    public static void controlVehicleToEntityTarget(EntityMaid maid, MobileVehicleEntity vehicle, Vec3 targetPos) {
        if (vehicle == null || !vehicle.isAlive()) return;

        long startTime = System.nanoTime();

        // 计算方向向量
        Vec3 toTarget = targetPos.subtract(vehicle.position()).multiply(1, 0, 1);
        double distance = toTarget.length();

        Entity ownerVehicle = maid.getOwner().getVehicle();
        Vec3 ownerPos = maid.getOwner().position();
        Vec3 toOwner = ownerPos.subtract(vehicle.position()).multiply(1, 0, 1);
        double distanceToOwner = toOwner.length();

        // 非常接近主人，停止
        if (ownerVehicle != null && vehicle.getUUID() != ownerVehicle.getUUID() && distanceToOwner < 8.0) {
            stopVehicle(vehicle);
            return;
        } else if (ownerVehicle == null && distanceToOwner < 8.0) {
            stopVehicle(vehicle);
            return;
        }

        toTarget = toTarget.normalize();
        toOwner = toOwner.normalize();

        // 获取载具当前朝向
        float yawRad = vehicle.getYRot() * Mth.DEG_TO_RAD;
        Vec3 vehicleForward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));

        // 计算方向关系
        double dot = vehicleForward.dot(toTarget);
        double cross = vehicleForward.x * toTarget.z - vehicleForward.z * toTarget.x;

        // 获取上次的转向决策并应用平滑
        UUID vehicleUUID = vehicle.getUUID();
        double lastCross = lastTurnDecision.getOrDefault(vehicleUUID, 0.0);
        double smoothFactor = Mth.clamp(distance / 30.0, 0.05, 0.2); // 修改平滑因子
        double smoothedCross = Mth.lerp(smoothFactor, lastCross, cross);
        lastTurnDecision.put(vehicleUUID, smoothedCross);
        int consistencyCount = turnConsistencyCounter.getOrDefault(vehicleUUID, 0);

        // 如果转向方向与上次相同，增加转向强度
        if (Math.signum(cross) == Math.signum(lastCross) && Math.abs(cross) > 0.2) {
            consistencyCount = Math.min(consistencyCount + 1, 5);
        } else {
            consistencyCount = Math.max(consistencyCount - 2, 0);
        }

        // 应用转向强度因子
        double distanceBasedTurnFactor = Mth.clamp(distance / 20.0, 0.5, 2.0);
        double turnFactor = 1.0 + (consistencyCount * 0.15) * distanceBasedTurnFactor;

        // 更新记录
        lastTurnDecision.put(vehicleUUID, cross);
        turnConsistencyCounter.put(vehicleUUID, consistencyCount);

        // 初始化控制指令
        short keys = 0;

        // 转向逻辑：更智能的转向
        double turnThreshold = 0.15; // 转向阈值

        // 当目标在后方时的转向策略
        if (dot < 0) {
            double reverseTurnFactor = 1.0 + (0.5 * (1.0 + dot));
            turnFactor *= reverseTurnFactor;

            if (dot < -0.8) {
                turnFactor *= 1.8;
            }
        }

        // 根据速度调整转向阈值，速度越快转向越敏感
        double currentSpeed = vehicle.getDeltaMovement().length();
        double speedFactor = Mth.clamp(currentSpeed / 2.0, 0.5, 1.5);
        double adjustedTurnThreshold = turnThreshold / (turnFactor * speedFactor);

        if (Math.abs(cross) > adjustedTurnThreshold) {
            if (cross > 0) {
                keys |= 0b000000001; // 左转
            } else {
                keys |= 0b000000010; // 右转
            }

            // 大幅转向时减少前进动力
            if (Math.abs(cross) > 0.5) {
                dot *= 0.6; // 减少前进动力
            }
        }

        // 移动逻辑
        if (distance > 32.0) {
            // 远距离：主要前进
            if (dot > 0.3) {
                keys |= 0b000000100; // 前进
            } else if (dot < -0.7) {
                keys |= 0b000001000; // 后退
            } else {
                keys |= 0b000000100; // 默认前进
            }

            // 转向逻辑
            if (Math.abs(cross) > 0.2) {
                if (cross > 0) {
                    keys |= 0b000000001; // 左转
                } else {
                    keys |= 0b000000010; // 右转
                }
            }
        } else {
            // 近距离：精细控制
            if (dot > 0.8) {
                keys |= 0b000000100; // 前进
            } else if (dot < -0.8) {
                keys |= 0b000001000; // 后退
            }

            // 近距离更需要转向
            if (Math.abs(cross) > 0.1) {
                if (cross > 0) {
                    keys |= 0b000000001; // 左转
                } else {
                    keys |= 0b000000010; // 右转
                }
            }
        }

        // 添加转向超时处理
        long currentTime = System.currentTimeMillis();
        long turnDuration = currentTime - turnStartTime.getOrDefault(vehicleUUID, currentTime);

        // 如果转向持续时间超过1.5秒且转向进度不足，尝试反向移动辅助转向
        if (turnDuration > 1500 && Math.abs(cross) > Math.abs(initialCross.getOrDefault(vehicleUUID, 0.0)) * 0.7) {
            // 转向效果不佳，尝试反向移动辅助转向
            if (dot > 0) {
                keys |= 0b000001000; // 后退
            } else {
                keys |= 0b000000100; // 前进
            }

            // 重置转向计时器
            turnStartTime.put(vehicleUUID, currentTime);
            initialCross.put(vehicleUUID, cross);
        } else if (Math.abs(cross) < 0.05) {
            // 转向完成，重置计时器
            turnStartTime.remove(vehicleUUID);
            initialCross.remove(vehicleUUID);
        } else if (!turnStartTime.containsKey(vehicleUUID)) {
            // 开始新转向，记录初始状态
            turnStartTime.put(vehicleUUID, currentTime);
            initialCross.put(vehicleUUID, cross);
        }

        // 速度稳定性处理
        double lastSpeed = speedHistory.getOrDefault(vehicleUUID, 0.0);
        double speedChange = Math.abs(currentSpeed - lastSpeed);

        // 如果速度变化过大，减少移动指令强度
        if (speedChange > 0.5 && currentSpeed > lastSpeed) {
            // 速度增加过快，减少前进指令
            if ((keys & 0b000000100) != 0) {
                // 每3帧只前进1帧，降低速度
                if (vehicle.tickCount % 3 != 0) {
                    keys &= ~0b000000100; // 移除前进指令
                }
            }
        }

        speedHistory.put(vehicleUUID, currentSpeed);

        // 防卡死逻辑
        Vec3 currentPos = vehicle.position();
        Vec3 lastPos = lastPositions.getOrDefault(vehicleUUID, currentPos);
        double positionChange = currentPos.distanceTo(lastPos);

        double speedThreshold = isInCrowd(maid, vehicle) ? 0.08 : 0.03;
        boolean isMakingProgress = positionChange > speedThreshold * 2;
        boolean isActuallyStuck = currentSpeed < speedThreshold && positionChange < speedThreshold && distance > 2.0;

        if (isActuallyStuck && !isMakingProgress) {
            int stuckCount = stuckCounter.getOrDefault(vehicleUUID, 0) + 1;
            stuckCounter.put(vehicleUUID, stuckCount);

            if (stuckCount > 40) {
                if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
                    MaidBeside.LOGGER.debug("Vehicle might be stuck, attempting escape maneuver");
                }

                // 尝试完全相反的策略
                short escapeKeys = 0;

                if (dot > 0) {
                    escapeKeys |= 0b000001000; // 后退
                } else {
                    escapeKeys |= 0b000000100; // 前进
                }

                // 完全相反的转向
                if (smoothedCross > 0) {
                    escapeKeys |= 0b000000010; // 右转
                } else {
                    escapeKeys |= 0b000000001; // 左转
                }

                // 应用逃脱策略
                vehicle.leftInputDown = (escapeKeys & 0b000000001) != 0;
                vehicle.rightInputDown = (escapeKeys & 0b000000010) != 0;
                vehicle.forwardInputDown = (escapeKeys & 0b000000100) != 0;
                vehicle.backInputDown = (escapeKeys & 0b000001000) != 0;

                // 限制防卡死操作的持续时间
                stuckCounter.put(vehicleUUID, 0);

                if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
                    MaidBeside.LOGGER.debug("Escape maneuver: Original keys = {}, New keys = {}", String.format("%9s", Integer.toBinaryString(keys)).replace(' ', '0'), String.format("%9s", Integer.toBinaryString(escapeKeys)).replace(' ', '0'));
                }
            }
        } else if (currentSpeed > speedThreshold * 2 || positionChange > speedThreshold * 3) {
            // 只有在真正移动时才重置计数器
            stuckCounter.put(vehicleUUID, 0);
        }

        lastPositions.put(vehicleUUID, currentPos);

        // 如果目标过近，强制远离
        if (distance < 8.0) {
            if (dot > 0) {
                keys |= 0b000000100; // 前进
            } else {
                keys |= 0b000001000; // 后退
            }

            if (cross > 0) {
                keys |= 0b000000001; // 左转
            } else {
                keys |= 0b000000010; // 右转
            }
        }

        vehicle.leftInputDown = (keys & 0b000000001) != 0;
        vehicle.rightInputDown = (keys & 0b000000010) != 0;
        vehicle.forwardInputDown = (keys & 0b000000100) != 0;
        vehicle.backInputDown = (keys & 0b000001000) != 0;

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000; // 微秒

        if (duration > 1000) { // 如果处理时间超过1毫秒
            MaidBeside.LOGGER.warn("Vehicle control took {} μs, consider optimizing", duration);
        }

        // 调试输出
        if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
            MaidBeside.LOGGER.debug("Vehicle - Pos: {}, Yaw: {}, Speed: {}", vehicle.position(), vehicle.getYRot(), vehicle.getDeltaMovement().length());
            MaidBeside.LOGGER.debug("ControlVehicle - Dot: {}({}), Cross: {}({}), Distance: {}", dot, getDirectionDescription(dot), cross, getTurnDescription(cross), distance);
            MaidBeside.LOGGER.debug("Strategy: {}, Keys: {}", getStrategyDescription(distance, dot), String.format("%9s", Integer.toBinaryString(keys)).replace(' ', '0'));
        }
    }

    private static boolean isInCrowd(EntityMaid maid, MobileVehicleEntity vehicle) {
        // 检测周围是否有大量实体
        AABB area = vehicle.getBoundingBox().inflate(3.0);
        int entityCount = vehicle.level().getEntities(EntityTypeTest.forClass(LivingEntity.class), area,
                e -> !e.is(maid) && !e.is(vehicle) && e.isAlive()).size();

        return entityCount > 3; // 周围有3个以上生物视为在人群中
    }

    public static void controlVehicleHybrid(EntityMaid maid, MobileVehicleEntity vehicle, Vec3 targetPos) {
        if (vehicle == null || !vehicle.isAlive()) return;

        // 计算方向向量（忽略Y轴）
        Vec3 toTarget = targetPos.subtract(vehicle.position()).multiply(1, 0, 1);
        double distance = toTarget.length();

        Entity ownerVehicle = maid.getOwner().getVehicle();
        Vec3 ownerPos = maid.getOwner().position();
        Vec3 toOwner = ownerPos.subtract(vehicle.position()).multiply(1, 0, 1);
        double distanceToOwner = toOwner.length();

        // 非常接近主人，停止
        if (ownerVehicle != null && vehicle.getUUID() != ownerVehicle.getUUID() && distanceToOwner < 8.0) {
            stopVehicle(vehicle);
            return;
        } else if (ownerVehicle == null && distanceToOwner < 8.0) {
            stopVehicle(vehicle);
            return;
        }

        // 非常接近目标，停止
        if (distance < 8.0) {
            stopVehicle(vehicle);
            return;
        }

        toTarget = toTarget.normalize();
        toOwner = toOwner.normalize();

        // 获取载具当前朝向
        float yawRad = vehicle.getYRot() * Mth.DEG_TO_RAD;
        Vec3 vehicleForward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));

        // 计算方向关系
        double dot = vehicleForward.dot(toTarget);    // 前后关系（1=正前，-1=正后）
        double cross = vehicleForward.x * toTarget.z - vehicleForward.z * toTarget.x; // 左右关系（正=左，负=右）

        // 获取上次的转向决策并应用平滑
        UUID vehicleUUID = vehicle.getUUID();
        double lastCross = lastTurnDecision.getOrDefault(vehicleUUID, 0.0);
        double smoothFactor = Mth.clamp(distance / 30.0, 0.05, 0.2); // 修改平滑因子
        double smoothedCross = Mth.lerp(smoothFactor, lastCross, cross);
        lastTurnDecision.put(vehicleUUID, smoothedCross);

        // 目标重评估机制 - 如果持续远离目标，重新计算路径
        double lastDistance = distanceHistory.getOrDefault(vehicleUUID, distance);
        boolean isGettingCloser = distance < lastDistance;
        float powerLevel = vehicle.getEntityData().get(MobileVehicleEntity.POWER);

        short keys = 0;
        double absCross = Math.abs(smoothedCross);

        // 转向逻辑 - 动态调整的转向阈值
        double adaptiveTurnThreshold = Mth.clamp(distance / 30.0, 0.05, 0.2);
        if (absCross > adaptiveTurnThreshold) {
            if (smoothedCross > 0) {
                keys |= 0b000000001; // 左转
            } else {
                keys |= 0b000000010; // 右转
            }
        }

        // 前进/后退逻辑
        if (dot > 0.3) {
            // 目标在前方或侧前方，优先前进
            keys |= 0b000000100; // 前进

            // 如果目标在侧前方且角度较大，增加转向
            if (dot < 0.7 && absCross > adaptiveTurnThreshold * 0.5) {
                if (smoothedCross > 0) {
                    keys |= 0b000000001; // 左转
                } else {
                    keys |= 0b000000010; // 右转
                }
            }
        } else if (dot < -0.7 && distance < 15.0) {
            // 目标在正后方且距离较近，后退
            keys |= 0b000001000; // 后退

            // 后退时转向方向与目标方向相反
            if (absCross > adaptiveTurnThreshold) {
                if (smoothedCross > 0) {
                    keys |= 0b000000010; // 右转（目标在左后方）
                } else {
                    keys |= 0b000000001; // 左转（目标在右后方）
                }
            }
        } else {
            // 其他情况：目标在侧后方或正后方但距离较远，优先后退并转向
            keys |= 0b000001000; // 后退

            // 增加转向力度
            if (absCross > adaptiveTurnThreshold * 0.3) {
                if (smoothedCross > 0) {
                    keys |= 0b000000001; // 左转
                } else {
                    keys |= 0b000000010; // 右转
                }
            }
        }

        // 直接设置输入状态，而不是通过 processInput
        vehicle.leftInputDown = (keys & 0b000000001) != 0;    // 左转
        vehicle.rightInputDown = (keys & 0b000000010) != 0;   // 右转
        vehicle.forwardInputDown = (keys & 0b000000100) != 0; // 前进
        vehicle.backInputDown = (keys & 0b000001000) != 0;    // 后退

        if (maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty()) {
            // 适度提升动力
            float currentPower = vehicle.getEntityData().get(MobileVehicleEntity.POWER);
            float targetPower = currentPower;
            if (vehicle.forwardInputDown) {
                // 前进时增加动力
                targetPower = Math.min(currentPower + 0.05f, 1.0f);
            } else if (vehicle.backInputDown) {
                // 后退时增加反向动力
                targetPower = Math.max(currentPower - 0.05f, -1.0f);
            } else {
                // 没有移动指令时逐渐减速
                targetPower = currentPower * 0.95f;
                if (Math.abs(targetPower) < 0.01f) targetPower = 0f;
            }

            // 设置动力
            vehicle.getEntityData().set(MobileVehicleEntity.POWER, targetPower);
        }

        // 直接控制转向系统
        float targetDeltaRot = vehicle.getEntityData().get(MobileVehicleEntity.DELTA_ROT);

        // 根据转向需求计算目标转向
        if (vehicle.leftInputDown) {
            targetDeltaRot = -0.5f;
        } else if (vehicle.rightInputDown) {
            targetDeltaRot = 0.5f;
        }

        // 直接设置转向
        vehicle.getEntityData().set(MobileVehicleEntity.DELTA_ROT, targetDeltaRot);

        // 防卡死逻辑
        double currentSpeed = vehicle.getDeltaMovement().length();
        Vec3 currentPos = vehicle.position();
        Vec3 lastPos = lastPositions.getOrDefault(vehicleUUID, currentPos);
        double positionChange = currentPos.distanceTo(lastPos);

        double speedThreshold = isInCrowd(maid, vehicle) ? 0.08 : 0.03;
        boolean isMakingProgress = positionChange > speedThreshold * 2;
        boolean isActuallyStuck = currentSpeed < speedThreshold && positionChange < speedThreshold && distance > 2.0;

        if (isActuallyStuck && !isMakingProgress) {
            int stuckCount = stuckCounter.getOrDefault(vehicleUUID, 0) + 1;
            stuckCounter.put(vehicleUUID, stuckCount);

            if (stuckCount > 40) {
                if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
                    MaidBeside.LOGGER.debug("Vehicle might be stuck, attempting escape maneuver");
                }

                // 尝试完全相反的策略
                short escapeKeys = 0;

                if (dot > 0) {
                    escapeKeys |= 0b000001000; // 后退
                } else {
                    escapeKeys |= 0b000000100; // 前进
                }

                // 完全相反的转向
                if (smoothedCross > 0) {
                    escapeKeys |= 0b000000010; // 右转
                } else {
                    escapeKeys |= 0b000000001; // 左转
                }

                // 应用逃脱策略
                vehicle.leftInputDown = (escapeKeys & 0b000000001) != 0;
                vehicle.rightInputDown = (escapeKeys & 0b000000010) != 0;
                vehicle.forwardInputDown = (escapeKeys & 0b000000100) != 0;
                vehicle.backInputDown = (escapeKeys & 0b000001000) != 0;

                // 限制防卡死操作的持续时间
                stuckCounter.put(vehicleUUID, 0);

                if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
                    MaidBeside.LOGGER.debug("Escape maneuver: Original keys = {}, New keys = {}", String.format("%9s", Integer.toBinaryString(keys)).replace(' ', '0'), String.format("%9s", Integer.toBinaryString(escapeKeys)).replace(' ', '0'));
                }
            }
        } else if (currentSpeed > speedThreshold * 2 || positionChange > speedThreshold * 3) {
            // 只有在真正移动时才重置计数器
            stuckCounter.put(vehicleUUID, 0);
        }

        lastPositions.put(vehicleUUID, currentPos);

        // 调试输出
        if (MaidConfig.MAID_BESIDE_DEBUG.get() && vehicle.tickCount % 10 == 0) {
            String movementStatus = isActuallyStuck ? "STUCK" :
                    isMakingProgress ? "MOVING" : "SLOW";
            String approachStatus = isGettingCloser ? "CLOSER" : "AWAY";

            MaidBeside.LOGGER.debug(
                    "ControlHybrid - Dot: {}({}), Cross: {}({}), SmoothedCross: {}, Distance: {}, Speed: {}, Keys: {}, Power: {}, Status: {}-{}",
                    dot, getDirectionDescription(dot),
                    cross, getTurnDescription(cross),
                    smoothedCross, distance, currentSpeed,
                    String.format("%9s", Integer.toBinaryString(keys)).replace(' ', '0'),
                    powerLevel, movementStatus, approachStatus
            );
        }
    }

    public static void stopVehicle(MobileVehicleEntity vehicle) {
        if (vehicle == null || !vehicle.isAlive()) return;

        // 直接设置载具的动力和转向为0
        vehicle.getEntityData().set(MobileVehicleEntity.POWER, 0f);
        vehicle.getEntityData().set(MobileVehicleEntity.DELTA_ROT, 0f);

        // 重置所有输入状态
        vehicle.leftInputDown = false;
        vehicle.rightInputDown = false;
        vehicle.forwardInputDown = false;
        vehicle.backInputDown = false;
        vehicle.upInputDown = false;
        vehicle.downInputDown = false;
        vehicle.sprintInputDown = false;

        VehicleHandler.cleanupVehicleState(vehicle.getUUID());

        if (MaidConfig.MAID_BESIDE_DEBUG.get()) {
            // MaidBeside.LOGGER.debug("VehicleHandler - Vehicle stopped forcefully");
        }
    }

    private static String getDirectionDescription(double dot) {
        if (dot > 0.7) return "FRONT";
        if (dot > 0.3) return "FRONT-SIDE";
        if (dot > -0.3) return "SIDE";
        if (dot > -0.7) return "BACK-SIDE";
        return "BACK";
    }

    private static String getTurnDescription(double cross) {
        if (cross > 0.3) return "TURN_LEFT";
        if (cross > -0.3) return "STRAIGHT";
        return "TURN_RIGHT";
    }

    private static String getStrategyDescription(double distance, double dot) {
        if (distance > 39) return dot > 0 ? "APPROACH" : "REPOSITION";
        if (distance > 21) return dot > 0.3 ? "ENGAGE" : "REPOSITION";
        if (distance > 13) return dot > 0.6 ? "CLOSE_COMBAT" : "CIRCLE";
        return dot > 0.7 ? "PURSUE" : "EVADE";
    }

    public static void cleanupVehicleState(UUID vehicleUUID) {
        lastTurnDecision.remove(vehicleUUID);
        turnConsistencyCounter.remove(vehicleUUID);
        turnStartTime.remove(vehicleUUID);
        initialCross.remove(vehicleUUID);
        speedHistory.remove(vehicleUUID);
        stuckCounter.remove(vehicleUUID);
        lastPositions.remove(vehicleUUID);
        distanceHistory.remove(vehicleUUID);
    }
}