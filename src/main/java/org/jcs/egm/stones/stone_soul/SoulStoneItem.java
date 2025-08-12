package org.jcs.egm.stones.stone_soul;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.StoneHolderItem;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityRegistries;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.stones.StoneItem;

import java.util.UUID;

public class SoulStoneItem extends StoneItem {

    public SoulStoneItem(Properties props) { super(props); }

    @Override public String getKey()   { return "soul"; }
    @Override public int    getColor() { return 0xFF7F00; }

    // ===== Active ability dispatch (sanitized) ============================================

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(getKey(), stack);
        if (ability == null) return InteractionResultHolder.pass(stack);

        if (StoneAbilityCooldowns.guardUse(player, stack, getKey(), ability)) {
            return InteractionResultHolder.pass(stack);
        }
        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!world.isClientSide) {
            ability.activate(world, player, stack);
            StoneAbilityCooldowns.apply(player, stack, getKey(), ability);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level world, LivingEntity entity, ItemStack stack, int count) {
        if (!(entity instanceof Player player)) return;
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(getKey(), stack);
        if (ability != null && ability.canHoldUse()) ability.onUsingTick(world, player, stack, count);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(getKey(), stack);
        if (ability != null && ability.canHoldUse()) {
            ability.releaseUsing(world, player, stack, timeLeft);
            if (!world.isClientSide) StoneAbilityCooldowns.apply(player, stack, getKey(), ability);
        }
    }

    // ===== Passive: Totem-of-Undying save =================================================
    // When lethal damage would kill a player who has the Soul Stone (raw / holder / gauntlet),
    // cancel death, apply classic totem-like buffs, and start a container-aware cooldown
    // under ability key "passive_totem" (register its base cooldown in StoneAbilityCooldowns).
    @Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class PassiveEvents {

        @SubscribeEvent
        public static void onLethal(LivingDeathEvent evt) {
            if (!(evt.getEntity() instanceof Player player)) return;
            if (player.level().isClientSide) return;

            ItemStack stoneStack = findSoulStoneStack(player);
            if (stoneStack.isEmpty()) return;

            int left = StoneAbilityCooldowns.remaining(player, "soul", "passive_totem");
            if (left > 0) return;

            evt.setCanceled(true);
            player.setHealth(1.0F);
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 3));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

            // A Soul For A Soul: Kill the closest entity as payment for revival
            killClosestEntity(player);

            player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.4, 0.6, 0.4, 0.02);
            }

            // Apply container-aware cooldown + persistent gate; overlay to active container
            var cdItem = StoneAbilityCooldowns.pickCooldownItem(player, stoneStack);
            StoneAbilityCooldowns.apply(player, cdItem, "soul", "passive_totem");
        }

        private static ItemStack findSoulStoneStack(Player p) {
            // raw
            for (ItemStack s : p.getInventory().items) {
                if (s.getItem() == ModItems.SOUL_STONE.get()) return s;
            }
            // holder
            for (ItemStack inv : p.getInventory().items) {
                if (inv.getItem() instanceof StoneHolderItem) {
                    ItemStack inside = StoneHolderItem.getStone(inv);
                    if (!inside.isEmpty() && inside.getItem() == ModItems.SOUL_STONE.get()) return inside;
                }
            }
            // gauntlet
            for (ItemStack inv : p.getInventory().items) {
                if (inv.getItem() instanceof InfinityGauntletItem) {
                    for (int i = 0; i < 6; i++) {
                        ItemStack s = InfinityGauntletItem.getStoneStack(inv, i);
                        if (!s.isEmpty() && s.getItem() == ModItems.SOUL_STONE.get()) return s;
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        private static void killClosestEntity(Player player) {
            Level level = player.level();
            double searchRadius = 32.0; // Search within 32 blocks
            
            AABB searchArea = new AABB(
                player.getX() - searchRadius, player.getY() - searchRadius, player.getZ() - searchRadius,
                player.getX() + searchRadius, player.getY() + searchRadius, player.getZ() + searchRadius
            );
            
            LivingEntity closestEntity = null;
            double closestDistanceSq = Double.MAX_VALUE;
            
            // Find the closest living entity that isn't the player
            for (Entity entity : level.getEntities(player, searchArea)) {
                if (entity instanceof LivingEntity livingEntity && entity != player) {
                    double distanceSq = player.distanceToSqr(entity);
                    if (distanceSq < closestDistanceSq) {
                        closestDistanceSq = distanceSq;
                        closestEntity = livingEntity;
                    }
                }
            }
            
            // Kill the closest entity if one was found
            if (closestEntity != null) {
                // Create soul particles at the victim's location
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SOUL, 
                        closestEntity.getX(), closestEntity.getY() + closestEntity.getBbHeight() / 2, closestEntity.getZ(), 
                        20, 0.3, 0.3, 0.3, 0.05);
                }
                
                // Kill the entity instantly
                closestEntity.kill();
            }
        }
    }

    // ===== Admin command hook: reset passive cooldown by player UUID ======================
    public static void resetCooldown(UUID playerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return;

        // Clear the per-player persistent gate for the passive
        var pd = player.getPersistentData();
        var root = pd.getCompound("egm_cd");              // must match StoneAbilityCooldowns' NBT root
        String key = "soul:passive_totem";
        if (root.contains(key)) {
            root.remove(key);
            pd.put("egm_cd", root);
        }

        // Clear/shorten any visible overlay on relevant containers so UI matches immediately
        var soulItem = ModItems.SOUL_STONE.get();
        // raw stone overlay
        player.getCooldowns().addCooldown(soulItem, 1);
        // holder / gauntlet that currently contain the Soul Stone
        for (ItemStack inv : player.getInventory().items) {
            if (inv.isEmpty()) continue;
            if (inv.getItem() instanceof StoneHolderItem) {
                ItemStack inside = StoneHolderItem.getStone(inv);
                if (!inside.isEmpty() && inside.getItem() == soulItem) {
                    player.getCooldowns().addCooldown(inv.getItem(), 1);
                }
            } else if (inv.getItem() instanceof InfinityGauntletItem) {
                boolean hasSoul = false;
                for (int i = 0; i < 6; i++) {
                    ItemStack s = InfinityGauntletItem.getStoneStack(inv, i);
                    if (!s.isEmpty() && s.getItem() == soulItem) { hasSoul = true; break; }
                }
                if (hasSoul) {
                    player.getCooldowns().addCooldown(inv.getItem(), 1);
                }
            }
        }
    }
}
