package org.jcs.egm.stones;

import java.util.Map;

import org.jcs.egm.stones.stone_mind.MindStoneAbility;
import org.jcs.egm.stones.stone_space.SpaceStoneAbility;
import org.jcs.egm.stones.stone_power.PowerStoneAbility;
import org.jcs.egm.stones.stone_time.TimeStoneAbility;
import org.jcs.egm.stones.stone_soul.SoulStoneAbility;
import org.jcs.egm.stones.stone_reality.RealityStoneAbility;

public class StoneAbilities {
    public static final Map<String, IGStoneAbility> REGISTRY = Map.ofEntries(
            Map.entry("mind",    new MindStoneAbility()),
            Map.entry("space",   new SpaceStoneAbility()),
            Map.entry("power",   new PowerStoneAbility()),
            Map.entry("time",    new TimeStoneAbility()),
            Map.entry("soul",    new SoulStoneAbility()),
            Map.entry("reality", new RealityStoneAbility())
            // Optionally add "snap" entry here once you implement that ability
    );
}
