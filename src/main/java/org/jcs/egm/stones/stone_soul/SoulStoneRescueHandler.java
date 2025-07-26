package org.jcs.egm.stones.stone_soul;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class SoulStoneRescueHandler {
    private static final Map<UUID, Long> lastRescue = new HashMap<>();
    private static final int REZ_COOLDOWN_TIME = 2; // 2 hrs
    private static final long COOLDOWN_MILLIS = 1000L * 60 * 60 * REZ_COOLDOWN_TIME;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("soul_stone_rez_cooldown_reset")
                        .requires(source -> source.hasPermission(2)) // OP-only, change as needed
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            lastRescue.remove(player.getUUID());
                            player.sendSystemMessage(Component.literal("Soul Stone resurrection cooldown reset")
                                    .withStyle(style -> style
                                            .withItalic(true)));
                            return 1;
                        })
        );
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Only allow in main worlds, not in creative, not when already dead
        if (player.isCreative() || player.isSpectator()) return;

        // Does player have Soul Stone or Gauntlet+Stone?
        boolean found = false;
        for (ItemStack stack : player.getInventory().items) {
            // Adjust for your Soul Stone and Gauntlet items:
            if (stack.is(ModItems.SOUL_STONE.get()) || IGStoneAbility.isInGauntlet(player, stack)) {
                found = true;
                break;
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
