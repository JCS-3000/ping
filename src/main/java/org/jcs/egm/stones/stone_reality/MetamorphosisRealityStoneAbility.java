package org.jcs.egm.stones.stone_reality;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.stones.StoneItem;
import org.jcs.egm.stones.StoneUseDamage;

import java.util.Optional;

public class MetamorphosisRealityStoneAbility implements IGStoneAbility {

    @Override public String abilityKey() { return "metamorphosis"; }
    @Override public boolean canHoldUse() { return false; } // instant

    // Tunables (feel free to move to config)
    private static final double MAX_RANGE = 50.0D;
    private static final int DURATION_TICKS = 60 * 20; // 20s

    // Reuse the SAME SFX as WilledChaos
    private static final SoundEvent BEAM_SOUND   = SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "reality_stone_firing"));
    private static final SoundEvent CHANGE_SOUND = SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "reality_stone_change"));

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        // Centralized cooldown application (same as WilledChaos)
        Item cdItem = StoneAbilityCooldowns.pickCooldownItem(player, stack);
        if (StoneAbilityCooldowns.isCooling(player, cdItem)) return;
        StoneAbilityCooldowns.apply(player, cdItem, "reality", abilityKey());

        // Raw stone cost
        if (!StoneItem.isInGauntlet(player, stack)) {
            player.hurt(StoneUseDamage.get(level, player), 4.0F);
        }

        // Cast SFX
        level.playSound(null, player.blockPosition(), BEAM_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);

        // Ray setup (same beam style)
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 start = eye;
        Vec3 end = start.add(look.scale(MAX_RANGE));

        EntityHitResult entityResult = getEntityHitResult(level, player, start, end);
        BlockHitResult blockResult = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        double entityDist = entityResult != null ? entityResult.getLocation().distanceTo(start) : Double.POSITIVE_INFINITY;
        double blockDist  = blockResult != null ? blockResult.getLocation().distanceTo(start) : Double.POSITIVE_INFINITY;

        // Beam particles along the cast path
        Vec3 impact = (entityDist < blockDist && entityResult != null) ? entityResult.getLocation() : blockResult.getLocation();
        if (level instanceof ServerLevel server) {
            spawnBeamParticles(server, start, impact);
        }

        // Only morph if a valid entity was actually hit and not occluded by a closer block
        if (entityResult == null || !(entityDist < blockDist) || entityDist > MAX_RANGE) return;

        Entity hit = entityResult.getEntity();
        if (!(hit instanceof LivingEntity living) || (hit instanceof Player)) return;

        // Respect metamorphosis entity blacklist
        ResourceLocation srcId = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        if (srcId != null && (WilledChaosBlacklistHelper.isHardcodedEntityBlacklisted(srcId)
                || WilledChaosBlacklistHelper.isConfigEntityBlacklisted(srcId))) {
            return;
        }

        // Start the morph on server
        if (player instanceof ServerPlayer sp) {
            MorphData.startMorph(sp, srcId, DURATION_TICKS, MorphData.Flags.none());
        }

        // Change SFX + small burst at impact
        level.playSound(null, impact.x, impact.y, impact.z, CHANGE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level instanceof ServerLevel server) {
            spawnBurstParticles(server, impact);
        }
    }

    // === helpers copied to match WilledChaos behavior ===

    private EntityHitResult getEntityHitResult(Level level, Player player, Vec3 start, Vec3 end) {
        EntityHitResult result = null;
        double closest = MAX_RANGE;
        var list = level.getEntities(player,
                player.getBoundingBox()
                        .expandTowards(end.subtract(start))
                        .inflate(1.0D),
                e -> e instanceof LivingEntity && !(e instanceof Player));

        for (Entity entity : list) {
            AABB aabb = entity.getBoundingBox().inflate(0.3D);
            Optional<Vec3> intercept = aabb.clip(start, end);
            if (intercept.isPresent()) {
                double dist = start.distanceTo(intercept.get());
                if (dist < closest) {
                    closest = dist;
                    result = new EntityHitResult(entity, intercept.get());
                }
            }
        }
        return result;
    }

    private void spawnBeamParticles(ServerLevel level, Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        Vec3 dir = to.subtract(from).normalize();
        RandomSource rand = level.getRandom();
        int particlesPerStep = 8;
        for (double i = 0; i < dist; i += 0.25D) {
            Vec3 pos = from.add(dir.scale(i));
            level.sendParticles(ModParticles.REALITY_STONE_EFFECT_ONE.get(), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            for (int j = 0; j < particlesPerStep; j++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = 0.15 + rand.nextDouble() * 0.15;
                Vec3 up = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                Vec3 right = dir.cross(up).normalize();
                Vec3 up2 = dir.cross(right).normalize();
                Vec3 offset = right.scale(Math.cos(angle) * radius).add(up2.scale(Math.sin(angle) * radius));
                Vec3 particlePos = pos.add(offset);
                level.sendParticles(ModParticles.REALITY_STONE_EFFECT_ONE.get(), particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void spawnBurstParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticles.REALITY_STONE_EFFECT_ONE.get(), pos.x, pos.y, pos.z, 100, 0.25, 0.25, 0.25, 0.01);
    }
}
