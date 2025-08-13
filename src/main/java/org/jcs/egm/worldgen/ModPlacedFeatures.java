package org.jcs.egm.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.jcs.egm.egm;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> TRAPPED_STARLIGHT_ORE_PLACED_KEY = registerKey("trapped_starlight_ore");

    public static void bootstrap(BootstapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, TRAPPED_STARLIGHT_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.TRAPPED_STARLIGHT_ORE_KEY),
                ModOrePlacement.commonOrePlacement(12, // Slightly more common than diamond
                        HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(30))));
    }

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(egm.MODID, name));
    }

    private static void register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}