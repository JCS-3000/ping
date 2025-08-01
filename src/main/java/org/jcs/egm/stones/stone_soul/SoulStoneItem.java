package org.jcs.egm.stones.stone_soul;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.holders.StoneHolderItem;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulStoneItem extends StoneItem {
    @Override
    public String getKey() { return "soul"; }
    @Override
    public int getColor() { return 0xD5700E; }

    private static final Map<UUID, Long> lastRescue = new HashMap<>();
    private static final int REZ_COOLDOWN_TIME_HOURS = 2; // 2 hours
    private static final long COOLDOWN_MILLIS = 1000L * 60 * 60 * REZ_COOLDOWN_TIME_HOURS;

    public SoulStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = SoulStoneAbilityRegistry.getSelectedAbility(stack);
        if (ability == null) {
            return InteractionResultHolder.pass(stack);
        }
        // Only click ability at present
        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        } else if (!world.isClientSide) {
            ability.activate(world, player, stack);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    // Passive resurrection logic (subscribe globally, handle with static below)
    @Mod.EventBusSubscriber
    public static class PassiveHandler {
        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (player.isCreative() || player.isSpectator()) return;

            // Check for Soul Stone (raw, in holder, or in gauntlet)
            boolean found = false;
            for (ItemStack stack : player.getInventory().items) {
                // Raw Soul Stone or Gauntlet+Stone:
                if (stack.getItem() instanceof SoulStoneItem || StoneItem.isInGauntlet(player, stack)) {
                    found = true;
                    break;
                }
                // Soul Stone Holder (red skull) containing Soul Stone:
                if (stack.is(ModItems.SOUL_STONE_HOLDER.get())) {
                    ItemStack inside = StoneHolderItem.getStone(stack);
                    if (inside != null && inside.getItem() instanceof SoulStoneItem) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return;

            // Cooldown check
            long now = System.currentTimeMillis();
            long last = lastRescue.getOrDefault(player.getUUID(), 0L);
            if (now - last < COOLDOWN_MILLIS) return;

            // Trigger "rescue"
            lastRescue.put(player.getUUID(), now);

            // Prevent death
            event.setCanceled(true);

            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.clearFire();

            // Strong totem effects
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 15, 4)); // 15 sec, lvl 5
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 15, 1)); // 15 sec, lvl 2
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 15, 1)); // 15 sec, lvl 2

            player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.0F);
            player.sendSystemMessage(
                    Component.literal("You survived... but what did it cost?")
                            .withStyle(Style.EMPTY
                                    .withItalic(true)
                                    .withColor(0xD5700E))
            );
        }
    }

    // For command reset, expose a static method:
    public static void resetCooldown(UUID playerUUID) {
        lastRescue.remove(playerUUID);
    }
}
