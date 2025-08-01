package org.jcs.egm.stones.stone_space;

import net.minecraft.network.chat.Component;
import org.jcs.egm.stones.IGStoneAbility;
import java.util.ArrayList;
import java.util.List;

public class SpaceStoneAbilityRegistry {
    public static final IGStoneAbility ENDERIC_BEAM = new EndericBeamSpaceStoneAbility();

    private static final List<IGStoneAbility> ABILITIES = new ArrayList<>();
    static {
        ABILITIES.add(ENDERIC_BEAM);
    }

    public static List<IGStoneAbility> getAbilities() {
        return ABILITIES;
    }

    public static List<Component> getAbilityNames() {
        List<Component> names = new ArrayList<>();
        names.add(Component.literal("Enderic Beam"));
        return names;
    }

    public static IGStoneAbility getSelectedAbility(net.minecraft.world.item.ItemStack stack) {
        int idx = stack.getOrCreateTag().getInt("AbilityIndex");
        if (idx < 0 || idx >= ABILITIES.size()) return ABILITIES.get(0);
        return ABILITIES.get(idx);
    }
}
