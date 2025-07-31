package org.jcs.egm.stones.stone_mind;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.network.MindStonePackets;
import org.jcs.egm.network.C2SApplySuggestionEffect;

public class MindStoneAbility implements IGStoneAbility {
    public static final int COOLDOWN_TICKS = 20 * 10; // 10 seconds

    // Holds the pending suggestion effect to use on the next mob click (client only)
    public static MindStoneSuggestionEffect pendingEffect = null;

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide) return;
        if (player.isShiftKeyDown()) {
            Minecraft.getInstance().setScreen(new org.jcs.egm.client.MindStoneScreen());
        }
    }

    // Called client-side after menu selection & mob click
    public static void tryApplyEffectToEntity(Player player, LivingEntity target, MindStoneSuggestionEffect effect) {
        if (player.level().isClientSide) {
            MindStonePackets.sendToServer(new C2SApplySuggestionEffect(target.getId(), effect));
        }
    }

    // Server-side: receive effect application request
    public static void applyEffectServer(ServerPlayer player, LivingEntity target, MindStoneSuggestionEffect effect) {
        if (player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
            player.displayClientMessage(Component.literal("Mind Stone is on cooldown!"), true);
            return;
        }
        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), COOLDOWN_TICKS);

        switch (effect) {
            case GO_AWAY -> {
                if (target instanceof Mob mob) {
                    mob.getNavigation().stop();
                    mob.getLookControl().setLookAt(player, 180, 180);
                    mob.setTarget(null);
                    mob.setLastHurtByMob(null);
                    mob.getNavigation().moveTo(
                            mob.getX() + (mob.getX() - player.getX()) * 3,
                            mob.getY(),
                            mob.getZ() + (mob.getZ() - player.getZ()) * 3,
                            1.25D
                    );
                    mob.setNoAi(false);
                    mob.setAggressive(false);
                    mob.level().broadcastEntityEvent(mob, (byte) 14); // heart particles
                }
            }
            case DROP_HELD_ITEM -> {
                if (target.getMainHandItem() != ItemStack.EMPTY) {
                    target.spawnAtLocation(target.getMainHandItem().copy(), 0.25F);
                    target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
            case FREEZE -> {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                        200, // 10 seconds
                        6,   // Slowness VII
                        false, false, true
                ));
                target.level().broadcastEntityEvent(target, (byte) 10); // particles
            }
        }
        player.displayClientMessage(Component.literal("Mind Stone: " + effect.getDisplayName() + " used!"), true);
    }

    @Override
    public boolean canHoldUse() { return false; }
}
