package com.kamikaguya.maid_beside.compat.superbwarfare.ai.brain;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import net.neoforged.fml.ModList;

public class LittleMaidImpl implements ILittleMaid {
    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        if (ModList.get().isLoaded("superbwarfare")) {
            // 调用 manager 的 addExtraMaidBrain 方法注册ExtraMaidBrain
            manager.addExtraMaidBrain(new ExtraMaidBrain());
        }
    }
}