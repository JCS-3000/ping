package org.jcs.egm.worldgen;

import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModOrePlacement {
    public static List<PlacementModifier> orePlacement(PlacementModifier countPlacement, PlacementModifier heightRangePlacement) {
        return List.of(countPlacement, InSquarePlacement.spread(), heightRangePlacement, BiomeFilter.biome());
    }

    public static List<PlacementModifier> commonOrePlacement(int count, PlacementModifier heightRangePlacement) {
        return orePlacement(CountPlacement.of(count), heightRangePlacement);
    }

    public static List<PlacementModifier> rareOrePlacement(int chance, PlacementModifier heightRangePlacement) {
        return orePlacement(RarityFilter.onAverageOnceEvery(chance), heightRangePlacement);
    }
}