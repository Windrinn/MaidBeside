package com.kamikaguya.maid_beside.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GeneralConfig {
    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MaidConfig.init(builder);
        return builder.build();
    }
}