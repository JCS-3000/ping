package org.jcs.egm.stones.stone_time;

import net.minecraft.network.chat.Component;
import org.jcs.egm.stones.IGStoneAbility;
import java.util.ArrayList;
import java.util.List;

public class TimeStoneAbilityRegistry {
    public static final IGStoneAbility PACIFY = new PacifyTimeStoneAbility();

    private static final List<IGStoneAbility> ABILITIES = new ArrayList<>();
    static {
        ABILITIES.add(PACIFY);
    }

    public static List<IGStoneAbility> getAbilities() {
        return ABILITIES;
    }

    public static List<Component> getAbilityNames() {
        List<Component> names = new ArrayList<>();
        names.add(Component.literal("Pacify"));
        return names;
    }

    public static IGStoneAbility getSelectedAbility(net.minecraft.world.item.ItemStack stack) {
        int idx = stack.getOrCreateTag().getInt("AbilityIndex");
        if (idx < 0 || idx >= ABILITIES.size()) return ABILITIES.get(0);
        return ABILITIES.get(idx);
    }
}
