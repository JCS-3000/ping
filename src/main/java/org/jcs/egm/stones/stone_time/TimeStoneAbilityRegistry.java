package org.jcs.egm.stones.stone_time;

import net.minecraft.network.chat.Component;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.ArrayList;
import java.util.List;

public class TimeStoneAbilityRegistry {
    public static final IGStoneAbility PACIFY  = new PacifyTimeStoneAbility();
    public static final IGStoneAbility BUBBLE  = new TimeBubbleTimeStoneAbility();
    public static final IGStoneAbility FREEZE  = new TimeFreezeTimeStoneAbility();

    private static final List<IGStoneAbility> ABILITIES = new ArrayList<>();
    static {
        ABILITIES.add(PACIFY);        // index 0
        ABILITIES.add(BUBBLE);  // index 1
        ABILITIES.add(FREEZE);
        // index 2, 3... (for 4 total)
    }

    public static List<IGStoneAbility> getAbilities() {
        return ABILITIES;
    }

    public static List<Component> getAbilityNames() {
        List<Component> names = new ArrayList<>();
        names.add(Component.literal("Pacify"));             // 0
        names.add(Component.literal("Time Bubble"));
        names.add(Component.literal("Strange Magicks"));// 1 (matches ABILITIES)
        return names;
    }

    public static IGStoneAbility getSelectedAbility(net.minecraft.world.item.ItemStack stack) {
        int idx = stack.getOrCreateTag().getInt("AbilityIndex");
        if (idx < 0 || idx >= ABILITIES.size()) return ABILITIES.get(0);
        return ABILITIES.get(idx);
    }
}
