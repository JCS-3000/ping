package org.jcs.egm.stones.stone_mind;

import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jcs.egm.network.NetworkHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MindStonePossessionHandler {

    private static final Map<UUID, PossessionInfo> ACTIVE = new HashMap<>();

    public static void startPossession(Player player, LivingEntity target, int durationTicks) {
        if (ACTIVE.containsKey(player.getUUID())) return;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 3));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durationTicks, 4));

        PossessionInfo info = new PossessionInfo(player, target, durationTicks);
        ACTIVE.put(player.getUUID(), info);

        if (player instanceof ServerPlayer sp) {
            NetworkHandler.sendStartPossessionPacket(sp, durationTicks, target.getId());
        }

        MinecraftForge.EVENT_BUS.register(info);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Your mind seizes control...")
                        .withStyle(Style.EMPTY.withItalic(true).withColor(0xffdd00)),
                true
        );
    }

    public static void cancelPossession(Player player) {
        PossessionInfo info = ACTIVE.remove(player.getUUID());
        if (player instanceof ServerPlayer sp) {
            sp.setCamera(sp);
            NetworkHandler.sendEndPossessionPacket(sp);
        }
        if (info != null) info.cleanup();
    }

    public static boolean isPossessing(Player player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static PossessionInfo getPossessionInfo(ServerPlayer player) {
        return ACTIVE.get(player.getUUID());
    }

    public static class PossessionInfo {
        private final UUID playerId, entityId;
        private final int duration;
        private int ticks;
        private boolean ended = false;

        public PossessionInfo(Player player, LivingEntity entity, int duration) {
            this.playerId = player.getUUID();
            this.entityId = entity.getUUID();
            this.duration = duration;
            this.ticks = 0;
        }

        public UUID getEntityId() { return entityId; }

        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Player player = e.player;
            if (!player.getUUID().equals(playerId)) return;

            Level lvl = player.level();
            EntityPossessionController.handle(player, entityId);

            ticks++;
            if (ticks >= duration || EntityPossessionController.shouldCancel(player)) {
                endPossession(lvl, true);
            } else {
                LivingEntity mob = EntityPossessionController.getEntity(lvl, entityId);
                if (mob != null) {
                    EntityPossessionController.spawnMindParticles(lvl, player);
                    EntityPossessionController.spawnMindParticles(lvl, mob);
                }
            }
        }

        @SubscribeEvent
        public void onLivingDeath(LivingDeathEvent ev) {
            UUID died = ev.getEntity().getUUID();
            if (died.equals(entityId) || died.equals(playerId)) {
                endPossession(ev.getEntity().level(), false);
            }
        }

        private void endPossession(Level level, boolean stunned) {
            if (ended) return;
            ended = true;

            Player player = level.getPlayerByUUID(playerId);
            LivingEntity mob   = EntityPossessionController.getEntity(level, entityId);

            if (stunned && mob != null) {
                mob.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1));
            }

            if (player instanceof ServerPlayer sp) {
                sp.setCamera(sp);
                NetworkHandler.sendEndPossessionPacket(sp);
            }
            if (player != null) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("You return to your body.")
                                .withStyle(Style.EMPTY.withItalic(true).withColor(0xffdd00)),
                        true
                );
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.removeEffect(MobEffects.ABSORPTION);
            }

            if (mob != null) {
                EntityPossessionController.cleanup(mob);
            }

            MinecraftForge.EVENT_BUS.unregister(this);
        }

        public void cleanup() {
            Player player = null;
            for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks
                    .getCurrentServer().getPlayerList().getPlayers()) {
                if (p.getUUID().equals(playerId)) { player = p; break; }
            }
            if (player != null) {
                for (net.minecraft.server.level.ServerLevel lvl
                        : net.minecraftforge.server.ServerLifecycleHooks
                        .getCurrentServer().getAllLevels()) {
                    LivingEntity found = EntityPossessionController.getEntity(lvl, entityId);
                    if (found != null) {
                        EntityPossessionController.cleanup(found);
                        break;
                    }
                }
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.removeEffect(MobEffects.ABSORPTION);
            }
        }
    }
}
