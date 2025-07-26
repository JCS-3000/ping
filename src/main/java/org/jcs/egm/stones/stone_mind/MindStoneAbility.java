package org.jcs.egm.stones.stone_mind;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.config.ModCommonConfig;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneUseDamage;

import java.util.List;

public class MindStoneAbility implements IGStoneAbility {

    private static final int COOLDOWN_TICKS_HAND = 120;     // 6s raw
    private static final int COOLDOWN_TICKS_GAUNTLET = 40;  // 2s in gauntlet
    private static final int POSSESSION_DURATION_TICKS = 20 * 120; // 120s
    private static final double MAX_RANGE = 16.0D;
    private static final double BEAM_STEP = 0.3D;

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        boolean inGauntlet = IGStoneAbility.isInGauntlet(player, stack);

        int cooldown = inGauntlet ? COOLDOWN_TICKS_GAUNTLET : COOLDOWN_TICKS_HAND;
        ItemStack cooldownStack = (inGauntlet && stack != null && stack.getItem().getClass().getSimpleName().equals("InfinityGauntletItem"))
                ? stack
                : player.getMainHandItem();

        // Cooldown check (silently ignore)
        if (player.getCooldowns().isOnCooldown(cooldownStack.getItem())) return;

        player.getCooldowns().addCooldown(cooldownStack.getItem(), cooldown);

        if (!inGauntlet) {
            player.hurt(StoneUseDamage.get(level, player), 4.0F);
        }

        // Play activation sound
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.5F, 1.4F);

        // Raytrace for a living entity
        RaycastResult rayResult = raycastForEntity(level, player, MAX_RANGE);

        // Beam for all players
        if (!level.isClientSide) {
            spawnBeamParticles((ServerLevel) level, player, rayResult.hitPos);
        }

        // If no hit, inform player
        if (rayResult.hitEntity == null) {
            player.displayClientMessage(Component.literal("No valid target."), true);
            return;
        }

        // Blacklist check
        List<? extends String> blacklist = ModCommonConfig.MIND_STONE_ENTITY_BLACKLIST.get();
        ResourceLocation entityKey = rayResult.hitEntity.getType().builtInRegistryHolder().key().location();
        if (entityKey != null && blacklist.contains(entityKey.toString())) {
            player.displayClientMessage(Component.literal("This entity resists the Mind Stone!"), true);
            return;
        }

        // Possess!
        MindStonePossessionHandler.startPossession(player, rayResult.hitEntity, POSSESSION_DURATION_TICKS);
        player.playSound(SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 1.0F, 1.2F);
    }

    @Override
    public boolean canHoldUse() {
        return false;
    }

    // Helper class for raycast result
    private static class RaycastResult {
        public final LivingEntity hitEntity;
        public final Vec3 hitPos;
        public RaycastResult(LivingEntity e, Vec3 p) { this.hitEntity = e; this.hitPos = p; }
    }

    private RaycastResult raycastForEntity(Level level, Player player, double maxRange) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endVec = eyePos.add(lookVec.scale(maxRange));
        ClipContext context = new ClipContext(eyePos, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        HitResult hit = level.clip(context);

        double distance = maxRange;
        if (hit.getType() == HitResult.Type.BLOCK) {
            distance = hit.getLocation().distanceTo(eyePos);
        }

        Vec3 checkVec = eyePos.add(lookVec.scale(distance));
        LivingEntity closest = null;
        double closestDist = maxRange + 1;
        for (Entity entity : level.getEntities(player, player.getBoundingBox().expandTowards(lookVec.scale(distance)).inflate(1.0D),
                e -> e instanceof LivingEntity le && le.isAlive() && le != player)) {
            double dist = entity.position().distanceTo(eyePos);
            if (dist < closestDist) {
                closest = (LivingEntity) entity;
                closestDist = dist;
            }
        }

        Vec3 hitVec = closest != null ? closest.position().add(0, closest.getBbHeight() * 0.5, 0) : checkVec;
        return new RaycastResult(closest, hitVec);
    }

    private void spawnBeamParticles(ServerLevel level, Player player, Vec3 hitVec) {
        Vec3 start = player.getEyePosition();
        Vec3 diff = hitVec.subtract(start);
        double length = diff.length();
        Vec3 dir = diff.normalize();

        int steps = (int) (length / BEAM_STEP);
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(dir.scale(i * BEAM_STEP));
            level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            // Swap above line with your custom particle if desired:
            // level.sendParticles(YourParticles.REALITY_STONE_EFFECT_ONE.get(), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }
}
