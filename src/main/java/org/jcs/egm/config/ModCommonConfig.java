package org.jcs.egm.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public class ModCommonConfig {
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_STONE_BLOCK_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_STONE_ENTITY_BLACKLIST;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("RealityStone");

        // Blocks blacklist
        REALITY_STONE_BLOCK_BLACKLIST = builder
                .comment("Blocks the Reality Stone will never create. Format: modid:block_name")
                .defineListAllowEmpty(
                        "reality_stone_block_blacklist",
                        List.of(
                                "minecraft:bedrock",
                                "minecraft:command_block"
                        ),
                        obj -> obj instanceof String s && s.contains(":")
                );

        // Entities blacklist
        REALITY_STONE_ENTITY_BLACKLIST = builder
                .comment("Entities the Reality Stone will never create. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "reality_stone_entity_blacklist",
                        List.of(
                                "minecraft:ender_dragon",
                                "minecraft:wither"
                                // Add more as needed
                        ),
                        obj -> obj instanceof String s && s.contains(":")
                );

        builder.pop();
        COMMON_CONFIG = builder.build();
    }
}
