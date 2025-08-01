package org.jcs.egm.stones.stone_power;

import net.minecraft.network.chat.Component;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.ArrayList;
import java.util.List;

public class PowerStoneAbilityRegistry {
    // Register all Power Stone abilities here in order
    public static final IGStoneAbility INFINITE_LIGHTNING = new InfiniteLightningPowerStoneAbility();
    // public static final IGStoneAbility YOUR_NEXT_ABILITY = new YourNextPowerAbility();

    private static final List<IGStoneAbility> ABILITIES = new ArrayList<>();

    static {
        ABILITIES.add(INFINITE_LIGHTNING);
        // ABILITIES.add(YOUR_NEXT_ABILITY);
    }

    public static List<IGStoneAbility> getAbilities() {
        return ABILITIES;
    }

    public static List<Component> getAbilityNames() {
        List<Component> names = new ArrayList<>();
        names.add(Component.literal("Infinite Lightning"));
        // names.add(Component.literal("Your Next Ability"));
        return names;
    }

    public static IGStoneAbility getSelectedAbility(net.minecraft.world.item.ItemStack stack) {
        int idx = stack.getOrCreateTag().getInt("AbilityIndex");
        if (idx < 0 || idx >= ABILITIES.size()) return ABILITIES.get(0);
        return ABILITIES.get(idx);
    }
}
