package com.kamikaguya.maid_beside.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class MaidConfig {
    public static ForgeConfigSpec.IntValue MAID_DRIVE_SHOOT_DISTANCE;
    public static ForgeConfigSpec.IntValue MAID_SEARCH_DISTANCE;
    public static ForgeConfigSpec.BooleanValue MAID_BESIDE_DEBUG;

    public MaidConfig() {
    }

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("maid");
        builder.comment("Recognition distance of a maid under the drive task(0-512)");
        MAID_DRIVE_SHOOT_DISTANCE = builder.defineInRange("MaidDriveShootDistance", 128, 0, 512);
        builder.comment("Ping distance for search maids near the player(0-512)");
        MAID_SEARCH_DISTANCE = builder.defineInRange("MaidSearchDistance", 256, 0, 512);
        builder.comment("Debug log output");
        MAID_BESIDE_DEBUG = builder.define("MaidBesideDebug", false);
        builder.pop();
    }
}
