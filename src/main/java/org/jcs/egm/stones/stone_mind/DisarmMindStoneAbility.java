package org.jcs.egm.stones.stone_mind;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.List;

public class DisarmMindStoneAbility implements IGStoneAbility {
    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10));
        if (mobs.isEmpty()) {
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("No mobs nearby to disarm."), true);
            return;
        }
        for (Mob mob : mobs) {
            ItemStack held = mob.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                mob.spawnAtLocation(held);
            }
        }
        serverPlayer.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Nearby mobs dropped their held items."), true);
    }
    @Override
    public boolean canHoldUse() { return false; }
}
