package org.jcs.egm.stones.stone_power;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneUseDamage;

public class PowerStoneAbility implements IGStoneAbility {

    @Override
    public void activate(Level level, Player player, ItemStack gauntletStack) {

        // "The radiation is mostly gamma..." - Bruce Banner
        boolean hasGauntlet = false;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem ||
                off.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem) {
            hasGauntlet = true;
        }
        if (!hasGauntlet) {
            player.hurt(StoneUseDamage.get(level, player), 6.0F);
        }

        shootArrow(level, player);
    }

    @Override
    public boolean canHoldUse() {
        return true;
    }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack gauntletStack, int count) {
        System.out.println("[Stone DEBUG] onUsingTick: " + this.getClass().getSimpleName() + " count=" + count);
        // Fire every 10 ticks
        if (count % 10 == 0) {
            shootArrow(level, player);
        }
    }

    private void shootArrow(Level level, Player player) {
        if (!level.isClientSide) {
            Arrow arrow = new Arrow(level, player);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
            level.addFreshEntity(arrow);
        }
    }
}
