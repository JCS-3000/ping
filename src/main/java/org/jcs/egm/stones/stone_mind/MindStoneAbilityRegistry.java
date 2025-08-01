package org.jcs.egm.stones.stone_mind;

import net.minecraft.network.chat.Component;
import org.jcs.egm.stones.IGStoneAbility;
import java.util.ArrayList;
import java.util.List;

public class MindStoneAbilityRegistry {
    public static final IGStoneAbility GO_AWAY  = new CowerMindStoneAbility();
    public static final IGStoneAbility DROP_HELD_ITEM = new DisarmMindStoneAbility();
    public static final IGStoneAbility FREEZE   = new StunMindStoneAbility();
    public static final IGStoneAbility ENRAGE   = new EnrageMindStoneAbility();

    private static final List<IGStoneAbility> ABILITIES = new ArrayList<>();
    static {
        ABILITIES.add(GO_AWAY);
        ABILITIES.add(DROP_HELD_ITEM);
        ABILITIES.add(FREEZE);
        ABILITIES.add(ENRAGE);
    }

    public static List<IGStoneAbility> getAbilities() { return ABILITIES; }

    public static List<Component> getAbilityNames() {
        List<Component> names = new ArrayList<>();
        names.add(Component.literal("Cower"));
        names.add(Component.literal("Disarm"));
        names.add(Component.literal("Stun"));
        names.add(Component.literal("Enrage"));
        return names;
    }

    public static IGStoneAbility getSelectedAbility(net.minecraft.world.item.ItemStack stack) {
        int idx = stack.getOrCreateTag().getInt("AbilityIndex");
        if (idx < 0 || idx >= ABILITIES.size()) return ABILITIES.get(0);
        return ABILITIES.get(idx);
    }
}
