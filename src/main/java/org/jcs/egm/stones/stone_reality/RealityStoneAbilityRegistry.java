package org.jcs.egm.stones.stone_reality;

import net.minecraft.network.chat.Component;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.ArrayList;
import java.util.List;

public class RealityStoneAbilityRegistry {
    // Register all abilities for the reality stone
    public static final IGStoneAbility WILLED_CHAOS = new WilledChaosRealityStoneAbility();
    public static final IGStoneAbility METAMORPHOSIS = new MetamorphosisRealityStoneAbility();

    private static final List<IGStoneAbility> ABILITIES = new ArrayList<>();
    static {
        ABILITIES.add(WILLED_CHAOS);
        ABILITIES.add(METAMORPHOSIS);
    }

    public static List<IGStoneAbility> getAbilities() {
        return ABILITIES;
    }

    public static List<Component> getAbilityNames() {
        List<Component> names = new ArrayList<>();
        names.add(Component.literal("Willed Chaos"));
        names.add(Component.literal("Metamorphosis"));
        // names.add(Component.literal("Another Ability"));
        return names;
    }

    public static IGStoneAbility getSelectedAbility(net.minecraft.world.item.ItemStack stack) {
        int idx = stack.getOrCreateTag().getInt("AbilityIndex");
        if (idx < 0 || idx >= ABILITIES.size()) return ABILITIES.get(0);
        return ABILITIES.get(idx);
    }
}
