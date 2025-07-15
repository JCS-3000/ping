package org.jcs.egm.stones.stone_space;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneUseDamage;

public class SpaceStoneAbility implements IGStoneAbility {

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

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            ThrownEnderpearl pearl = new ThrownEnderpearl(serverLevel, player);
            pearl.setItem(new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL));
            pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            serverLevel.addFreshEntity(pearl);
        }
    }
}
