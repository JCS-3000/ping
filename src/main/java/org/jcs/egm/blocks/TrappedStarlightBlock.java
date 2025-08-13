package org.jcs.egm.blocks;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.DropExperienceBlock;

public class TrappedStarlightBlock extends DropExperienceBlock {
    
    public TrappedStarlightBlock(IntProvider xpRange, Properties properties) {
        super(properties, xpRange);
    }
}