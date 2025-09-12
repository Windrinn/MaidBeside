package com.kamikaguya.maid_beside.compat.superbwarfare;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.kamikaguya.maid_beside.compat.superbwarfare.task.TaskDriveVehicle;
import com.kamikaguya.maid_beside.registry.MaidBesideRegistry;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.List;

@LittleMaidExtension
public class MaidExtension implements ILittleMaid {

    @Override
    public void addMaidTask(TaskManager manager) {
        ILittleMaid.super.addMaidTask(manager);
        manager.add(new TaskDriveVehicle());
    }

    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        manager.addExtraMaidBrain(new IExtraMaidBrain() {
            @Override
            public List<MemoryModuleType<?>> getExtraMemoryTypes() {
                return List.of(
                        MaidBesideRegistry.PING_TARGET.get()
                );
            }
        });
    }
}