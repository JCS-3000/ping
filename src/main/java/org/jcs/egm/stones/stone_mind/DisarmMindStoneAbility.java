package org.jcs.egm.stones.stone_mind;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import java.util.List;

public class DisarmMindStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "disarm"; }

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        var cdItem = StoneAbilityCooldowns.pickCooldownItem(player, stack);
        if (StoneAbilityCooldowns.isCooling(player, cdItem)) return;

        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return;

        AABB area = player.getBoundingBox().inflate(10.0);

        boolean didAnything = false;

        // Disarm mobs (main hand only)
        List<Mob> mobs = server.getEntitiesOfClass(Mob.class, area);
        for (Mob mob : mobs) {
            ItemStack held = mob.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                mob.spawnAtLocation(held);
                didAnything = true;
            }
        }

        // Disarm other players (skip self, skip creative/spectator)
        List<ServerPlayer> players = server.getEntitiesOfClass(ServerPlayer.class, area);
        for (ServerPlayer other : players) {
            if (other.getUUID().equals(serverPlayer.getUUID())) continue;
            if (other.isCreative() || other.isSpectator()) continue;

            ItemStack main = other.getMainHandItem();
            if (!main.isEmpty()) {
                ItemStack toDrop = main.copy();
                other.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                other.getInventory().setChanged();
                other.drop(toDrop, true);
                didAnything = true;
            }
        }

        if (didAnything) {
            StoneAbilityCooldowns.apply(player, cdItem, "mind", abilityKey());
        }
    }

    @Override
    public boolean canHoldUse() { return false; }
}
