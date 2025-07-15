package org.jcs.egm.stones.stone_mind;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;

public class MindStoneAbility implements IGStoneAbility {

    @Override
    public void activate(Level level, Player player, ItemStack gauntletStack) {
        spawnHearts(level, player);
    }

    @Override
    public boolean canHoldUse() {
        return true;
    }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack gauntletStack, int count) {
        if (count % 5 == 0) {
            spawnHearts(level, player);
        }
    }

    private void spawnHearts(Level level, Player player) {
        if (!level.isClientSide) return;

        double x = player.getX();
        double y = player.getY() + 2.0;
        double z = player.getZ();

        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.HEART, x, y, z, 0, 0.1, 0);
        }
    }
}
