package org.jcs.egm.worldgen;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;
import org.jcs.egm.egm;

public class ModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_TRAPPED_STARLIGHT_ORE = registerKey("add_trapped_starlight_ore");

    public static void bootstrap(BootstapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        register(context, ADD_TRAPPED_STARLIGHT_ORE, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "the_end"))),
                        biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "end_highlands"))),
                        biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "end_midlands"))),
                        biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "small_end_islands"))),
                        biomes.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "end_barrens")))),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.TRAPPED_STARLIGHT_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(egm.MODID, name));
    }

    private static void register(BootstapContext<BiomeModifier> context, ResourceKey<BiomeModifier> key, BiomeModifier biomeModifier) {
        context.register(key, biomeModifier);
    }
}