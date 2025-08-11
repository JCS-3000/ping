package org.jcs.egm.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class ModCommonConfig {
    public static final ForgeConfigSpec COMMON_CONFIG;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_STONE_BLOCK_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_STONE_ENTITY_BLACKLIST;

    // Metamorphosis (Reality Stone) – NEW
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_METAMORPHOSIS_FLY_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_METAMORPHOSIS_SWIM_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_METAMORPHOSIS_FIREPROOF_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_METAMORPHOSIS_SLOWFALL_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALITY_METAMORPHOSIS_SPIDERCLIMB_ENTITIES;

    // Mind Stone (existing)
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MIND_STONE_ENTITY_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MIND_STONE_FLIGHT_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MIND_STONE_WALLCLIMB_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MIND_STONE_SWIMSPEED_ENTITIES;

    public static final ForgeConfigSpec.BooleanValue ENABLE_CAMERA_SHAKE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // ---------- General ----------
        builder.push("General");
        ENABLE_CAMERA_SHAKE = builder
                .comment("If true, certain abilities (e.g., Shockwave Slam) will trigger a client-side camera shake.")
                .define("enable_camera_shake", true);
        builder.pop();

        // ---------- Reality Stone ----------
        builder.push("RealityStone");

        REALITY_STONE_BLOCK_BLACKLIST = builder
                .comment("Blocks the Reality Stone will never create. Format: modid:block_name")
                .defineListAllowEmpty(
                        "reality_stone_block_blacklist",
                        List.of("minecraft:bedrock", "minecraft:command_block"),
                        obj -> obj instanceof String s && s.contains(":"));

        REALITY_STONE_ENTITY_BLACKLIST = builder
                .comment("Entities the Reality Stone will never create. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "reality_stone_entity_blacklist",
                        List.of("minecraft:ender_dragon", "minecraft:wither"),
                        obj -> obj instanceof String s && s.contains(":"));

        // ---- Metamorphosis (NEW) ----
        builder.push("Metamorphosis");

        REALITY_METAMORPHOSIS_FLY_ENTITIES = builder
                .comment("Entities that grant flight when morphed. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "fly_entities",
                        List.of(
                                "minecraft:bat","minecraft:bee","minecraft:parrot","minecraft:phantom",
                                "minecraft:ghast","minecraft:blaze","minecraft:vex","minecraft:allay",
                                "minecraft:wither","minecraft:ender_dragon"
                        ),
                        obj -> obj instanceof String s && s.contains(":"));

        REALITY_METAMORPHOSIS_SWIM_ENTITIES = builder
                .comment("Entities that grant strong swimming/water breathing when morphed. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "swim_entities",
                        List.of(
                                "minecraft:squid","minecraft:glow_squid","minecraft:axolotl","minecraft:dolphin",
                                "minecraft:cod","minecraft:salmon","minecraft:pufferfish","minecraft:tropical_fish",
                                "minecraft:turtle","minecraft:guardian","minecraft:elder_guardian"
                        ),
                        obj -> obj instanceof String s && s.contains(":"));

        REALITY_METAMORPHOSIS_FIREPROOF_ENTITIES = builder
                .comment("Entities that grant fire resistance when morphed. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "fireproof_entities",
                        List.of("minecraft:blaze","minecraft:wither","minecraft:ghast","minecraft:magma_cube","minecraft:strider"),
                        obj -> obj instanceof String s && s.contains(":"));

        REALITY_METAMORPHOSIS_SLOWFALL_ENTITIES = builder
                .comment("Entities that grant slow falling when morphed. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "slowfall_entities",
                        List.of("minecraft:chicken"),
                        obj -> obj instanceof String s && s.contains(":"));

        REALITY_METAMORPHOSIS_SPIDERCLIMB_ENTITIES = builder
                .comment("Entities that grant wall-climb when morphed. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "spiderclimb_entities",
                        List.of("minecraft:spider","minecraft:cave_spider"),
                        obj -> obj instanceof String s && s.contains(":"));

        builder.pop(); // Metamorphosis
        builder.pop(); // RealityStone

        // ---------- Mind Stone (existing) ----------
        builder.push("MindStone");

        MIND_STONE_ENTITY_BLACKLIST = builder
                .comment("Entities that cannot be possessed by the Mind Stone. Format: modid:entity_name")
                .defineListAllowEmpty(
                        "mind_stone_entity_blacklist",
                        List.of("minecraft:ender_dragon","minecraft:wither","minecraft:warden"),
                        obj -> obj instanceof String s && s.contains(":"));

        MIND_STONE_FLIGHT_ENTITIES = builder
                .comment("Entities that gain flight controls when possessed. Format: modid:entity_name.")
                .defineListAllowEmpty(
                        "mind_stone_flight_entities",
                        List.of("minecraft:bat","minecraft:blaze","minecraft:ghast","minecraft:parrot","minecraft:bee",
                                "minecraft:phantom","minecraft:vex","minecraft:ender_dragon","minecraft:allay"),
                        obj -> obj instanceof String s && s.contains(":"));

        MIND_STONE_WALLCLIMB_ENTITIES = builder
                .comment("Entities that gain wall-climb controls when possessed. Format: modid:entity_name.")
                .defineListAllowEmpty(
                        "mind_stone_wallclimb_entities",
                        List.of("minecraft:spider","minecraft:cave_spider","minecraft:silverfish","minecraft:endermite"),
                        obj -> obj instanceof String s && s.contains(":"));

        MIND_STONE_SWIMSPEED_ENTITIES = builder
                .comment("Entities that gain increased swim speed when possessed. Format: modid:entity_name.")
                .defineListAllowEmpty(
                        "mind_stone_swimspeed_entities",
                        List.of("minecraft:squid","minecraft:glow_squid","minecraft:axolotl","minecraft:dolphin",
                                "minecraft:cod","minecraft:salmon","minecraft:pufferfish","minecraft:tropical_fish","minecraft:turtle"),
                        obj -> obj instanceof String s && s.contains(":"));

        builder.pop(); // MindStone

        COMMON_CONFIG = builder.build();
    }
}
