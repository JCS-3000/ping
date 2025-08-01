package org.jcs.egm.stones.stone_soul;

import net.minecraft.network.chat.Component;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.ArrayList;
import java.util.List;

public class SoulStoneAbilityRegistry {
    // Only one ability for now: Soul Bind
    public static final IGStoneAbility SOUL_BIND = new SoulBindSoulStoneAbility();

    private static final List<IGStoneAbility> ABILITIES = new ArrayList<>();
    static {
        ABILITIES.add(SOUL_BIND);
    }

    public static List<IGStoneAbility> getAbilities() {
        return ABILITIES;
    }

    public static List<Component> getAbilityNames() {
        List<Component> names = new ArrayList<>();
        names.add(Component.literal("Soul Bind"));
        return names;
    }

    public static IGStoneAbility getSelectedAbility(net.minecraft.world.item.ItemStack stack) {
        int idx = stack.getOrCreateTag().getInt("AbilityIndex");
        if (idx < 0 || idx >= ABILITIES.size()) return ABILITIES.get(0);
        return ABILITIES.get(idx);
    }
}
