package com.kamikaguya.maid_beside.compat.superbwarfare.ai.brain;

import com.kamikaguya.maid_beside.compat.superbwarfare.task.TaskDriveVehicle;
import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;

import java.util.List;

public class ExtraMaidBrain implements IExtraMaidBrain {
    @Override
    public List<MemoryModuleType<?>> getExtraMemoryTypes() {
        // 返回新记忆类型
        return ImmutableList.of(TaskDriveVehicle.PING_TARGET());
    }

    @Override
    public List<SensorType<? extends Sensor<? super EntityMaid>>> getExtraSensorTypes() {
        return ImmutableList.of();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getCoreBehaviors() {
        return ImmutableList.of();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getIdleBehaviors() {
        return ImmutableList.of();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getWorkBehaviors() {
        return ImmutableList.of();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRestBehaviors() {
        return ImmutableList.of();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getPanicBehaviors() {
        return ImmutableList.of();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRideIdleBehaviors() {
        return ImmutableList.of();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRideWorkBehaviors() {
        return ImmutableList.of();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRideRestBehaviors() {
        return ImmutableList.of();
    }
}