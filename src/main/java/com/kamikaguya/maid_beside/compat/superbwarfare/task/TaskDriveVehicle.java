package com.kamikaguya.maid_beside.compat.superbwarfare.task;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunCommonUtil;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.ai.GunShootTargetTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.util.SoundUtil;
import com.google.common.collect.Lists;
import com.kamikaguya.maid_beside.compat.superbwarfare.handler.VehicleHandler;
import com.kamikaguya.maid_beside.config.MaidConfig;
import com.kamikaguya.maid_beside.main.MaidBeside;
import com.kamikaguya.maid_beside.registry.MaidBesideRegistry;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class TaskDriveVehicle implements IRangedAttackTask {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MaidBeside.MODID, "drive_vehicle");
    public static final ItemStack ICON = ModItems.MONITOR.get().getDefaultInstance();

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return ICON;
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return SoundUtil.attackSound(maid, InitSounds.MAID_RANGE_ATTACK.get(), 0.5f);
    }

    @Override
    public boolean enableLookAndRandomWalk(EntityMaid maid) {
        return false;
    }

    public static MemoryModuleType<BlockPos> PING_TARGET() {
        return MaidBesideRegistry.PING_TARGET.get();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        // 非骑乘状态下不需要驾驶行为
        return Collections.emptyList();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        // 骑乘状态下的驾驶行为
        BehaviorControl<EntityMaid> supplementedTask = StartAttacking.create(
                GunCommonUtil::canStartAttacking, IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<EntityMaid> gunWalkTargetTask = MaidRangedWalkToTarget.create(0.6f);
        BehaviorControl<EntityMaid> driveVehicleTargetTask = MaidRangedDriveToTarget.create();
        BehaviorControl<EntityMaid> findTargetTask = StopAttackingIfTargetInvalid.create(target ->
                !GunCommonUtil.canStartAttacking(maid));
        BehaviorControl<EntityMaid> gunShootTargetTask = new GunShootTargetTask();
        BehaviorControl<EntityMaid> driveToPingTask = new MaidDriveToPingTarget();
        BehaviorControl<EntityMaid> stopVehicleTask = StopVehicleIfNoTarget.create();


        return Lists.newArrayList(
                Pair.of(3, driveToPingTask),
                Pair.of(4, stopVehicleTask),
                Pair.of(5, supplementedTask),
                Pair.of(5, findTargetTask),
                Pair.of(5, gunWalkTargetTask),
                Pair.of(5, driveVehicleTargetTask),
                Pair.of(5, gunShootTargetTask)
        );
    }

    @Override
    public AABB searchDimension(EntityMaid maid) {
        if (GunCommonUtil.canStartAttacking(maid)) {
            float searchRange = this.searchRadius(maid);
            if (maid.hasRestriction()) {
                return new AABB(maid.getRestrictCenter()).inflate(searchRange);
            } else {
                return maid.getBoundingBox().inflate(searchRange);
            }
        }
        return IRangedAttackTask.super.searchDimension(maid);
    }

    @Override
    public float searchRadius(EntityMaid maid) {
        return MaidConfig.MAID_DRIVE_SHOOT_DISTANCE.get();
    }

    @Override
    public boolean canSee(EntityMaid maid, LivingEntity target) {
        return GunCommonUtil.canSee(maid, target)
                .orElseGet(() -> IRangedAttackTask.super.canSee(maid, target));
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Collections.singletonList(Pair.of("has_driving", this::isDrivingVehicle));
    }

    private boolean isDrivingVehicle(EntityMaid maid) {
        Entity vehicle = maid.getVehicle();
        return vehicle instanceof MobileVehicleEntity && VehicleHandler.isDriver(maid, vehicle);
    }

    @Override
    public void performRangedAttack(EntityMaid shooter, LivingEntity target, float distanceFactor) {
    }
}