package org.jcs.egm.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class ModCommonConfig {
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_STONE_BLOCK_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_STONE_ENTITY_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MIND_STONE_ENTITY_BLACKLIST;

    // Advanced movement config categories
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MIND_STONE_FLIGHT_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MIND_STONE_WALLCLIMB_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MIND_STONE_SWIMSPEED_ENTITIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // Reality Stone section
        builder.push("RealityStone");

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

        REALITY_STONE_ENTITY_BLACKLIST = builder
                .comment("Entities the Reality Stone will never create. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "reality_stone_entity_blacklist",
                        List.of(
                                "minecraft:ender_dragon",
                                "minecraft:wither"
                        ),
                        obj -> obj instanceof String s && s.contains(":")
                );

        builder.pop();

        // Mind Stone section
        builder.push("MindStone");

        MIND_STONE_ENTITY_BLACKLIST = builder
                .comment("Entities that cannot be possessed by the Mind Stone. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "mind_stone_entity_blacklist",
                        List.of(
                                "minecraft:ender_dragon",
                                "minecraft:wither",
                                "minecraft:warden"
                        ),
                        obj -> obj instanceof String s && s.contains(":")
                );

        MIND_STONE_FLIGHT_ENTITIES = builder
                .comment("Entities that gain flight controls when possessed. Format: modid:entity_name.")
                .defineListAllowEmpty(
                        "mind_stone_flight_entities",
                        List.of(
                                "minecraft:bat",
                                "minecraft:blaze",
                                "minecraft:ghast",
                                "minecraft:parrot",
                                "minecraft:bee",
                                "minecraft:phantom",
                                "minecraft:vex",
                                "minecraft:ender_dragon",
                                "minecraft:allay"
                        ),
                        obj -> obj instanceof String s && s.contains(":")
                );

        MIND_STONE_WALLCLIMB_ENTITIES = builder
                .comment("Entities that gain wall-climb controls when possessed. Format: modid:entity_name.")
                .defineListAllowEmpty(
                        "mind_stone_wallclimb_entities",
                        List.of(
                                "minecraft:spider",
                                "minecraft:cave_spider",
                                "minecraft:silverfish",
                                "minecraft:endermite"
                        ),
                        obj -> obj instanceof String s && s.contains(":")
                );

        MIND_STONE_SWIMSPEED_ENTITIES = builder
                .comment("Entities that gain increased swim speed when possessed. Format: modid:entity_name.")
                .defineListAllowEmpty(
                        "mind_stone_swimspeed_entities",
                        List.of(
                                "minecraft:squid",
                                "minecraft:glow_squid",
                                "minecraft:axolotl",
                                "minecraft:dolphin",
                                "minecraft:cod",
                                "minecraft:salmon",
                                "minecraft:pufferfish",
                                "minecraft:tropical_fish",
                                "minecraft:turtle"
                        ),
                        obj -> obj instanceof String s && s.contains(":")
                );

        builder.pop();

        COMMON_CONFIG = builder.build();
    }
}
