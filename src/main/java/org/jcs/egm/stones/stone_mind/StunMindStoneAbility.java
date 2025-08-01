package org.jcs.egm.stones.stone_mind;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.List;

public class StunMindStoneAbility implements IGStoneAbility {
    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10));
        if (targets.isEmpty()) {
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("No entities nearby to stun."), true);
            return;
        }
        for (LivingEntity ent : targets) {
            if (ent == player) continue; // Don't stun self
            ent.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 9)); // Level 10 slowness (0 indexed), 5 seconds
        }
        serverPlayer.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Nearby entities are stunned!"), true);
    }
    @Override
    public boolean canHoldUse() { return false; }
}
