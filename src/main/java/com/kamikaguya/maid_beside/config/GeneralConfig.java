package com.kamikaguya.maid_beside.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneralConfig {
    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MaidConfig.init(builder);
        return builder.build();
    }
}