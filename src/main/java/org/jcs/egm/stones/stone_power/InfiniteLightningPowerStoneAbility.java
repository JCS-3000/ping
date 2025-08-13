package org.jcs.egm.stones.stone_power;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.entity.PowerStoneLightningEntity;
import org.jcs.egm.particles.ChargingParticleHelper;
import org.jcs.egm.registry.ModEntities;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InfiniteLightningPowerStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "infinite_lightning"; }

    private final Map<UUID, PowerStoneLightningEntity> activeBeams = new HashMap<>();
    private final Map<UUID, Integer> bedrockHitTicks = new HashMap<>();
    private final Map<BlockPos, Integer> miningTicks = new HashMap<>();
    private final Map<UUID, BlockPos> lastBlockHit = new HashMap<>();

    // Rate limiting for “shots” while holding
    private final Map<UUID, Long> lastShotGameTime = new HashMap<>();

    // Client sound state
    private final Set<UUID> chargingSoundPlayers = new HashSet<>();
    private final Set<UUID> firingSoundPlayers = new HashSet<>();
    private final Map<UUID, Integer> firingSoundStartTick = new HashMap<>();

    private static final int FIRING_SOUND_LENGTH_TICKS = 160;
    private static final SoundEvent CHARGING_SOUND = SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("egm", "power_stone_charging"));
    private static final SoundEvent FIRING_SOUND   = SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("egm", "power_stone_firing"));

    private static final int BEDROCK_BREAK_TICKS = 300;

    @Override
    public void activate(Level level, Player player, ItemStack stack) {}

    @Override
    public boolean canHoldUse() { return true; }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        final String stone = "power";
        final String ability = abilityKey();

        int useDuration = player.getUseItem().getUseDuration();
        int ticksHeld = useDuration - count;
        int chargeTicks = StoneAbilityCooldowns.chargeup(stone, ability);
        UUID uuid = player.getUUID();

        // --- CHARGING PHASE ---
        if (ticksHeld < chargeTicks) {
            if (level.isClientSide && ticksHeld % 4 == 0) ChargingParticleHelper.spawnPowerSuckInParticles(level, player);
            if (level.isClientSide) {
                if (!chargingSoundPlayers.contains(uuid)) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            CHARGING_SOUND, SoundSource.PLAYERS, 0.8f, 1.0f, false);
                    chargingSoundPlayers.add(uuid);
                }
                if (firingSoundPlayers.contains(uuid)) {
                    Minecraft.getInstance().getSoundManager().stop(FIRING_SOUND.getLocation(), SoundSource.PLAYERS);
                    firingSoundPlayers.remove(uuid);
                    firingSoundStartTick.remove(uuid);
                }
            }
            return;
        }

        // --- FIRING PHASE (rate-limited) ---
        int interval = Math.max(1, StoneAbilityCooldowns.holdInterval(stone, ability));
        long now = level.getGameTime();
        Long last = lastShotGameTime.get(uuid);
        if (last != null && now - last < interval) {
            // Still update/keep beam visuals server-side for smoothness
            if (!level.isClientSide) updateBeamOnly(level, player, uuid);
            return;
        }
        lastShotGameTime.put(uuid, now);

        if (level.isClientSide) {
            if (chargingSoundPlayers.contains(uuid)) {
                Minecraft.getInstance().getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
                chargingSoundPlayers.remove(uuid);
            }
            int currentTick = Minecraft.getInstance().level != null ? (int)Minecraft.getInstance().level.getGameTime() : 0;
            Integer startedAt = firingSoundStartTick.get(uuid);
            boolean needsNewSound = !firingSoundPlayers.contains(uuid)
                    || startedAt == null
                    || (currentTick - startedAt) >= FIRING_SOUND_LENGTH_TICKS;
            if (needsNewSound) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        FIRING_SOUND, SoundSource.PLAYERS, 1.0f, 1.0f, true);
                firingSoundPlayers.add(uuid);
                firingSoundStartTick.put(uuid, currentTick);
            }
            return;
        }

        // ---- Server: raycast, damage/mining, and beam update ----
        fireOneInterval(level, player, uuid);
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int count) {
        UUID uuid = player.getUUID();

        // Stop sounds client-side
        if (level.isClientSide) {
            Minecraft.getInstance().getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
            Minecraft.getInstance().getSoundManager().stop(FIRING_SOUND.getLocation(), SoundSource.PLAYERS);
            chargingSoundPlayers.remove(uuid);
            firingSoundPlayers.remove(uuid);
            firingSoundStartTick.remove(uuid);
        }

        // Remove beam server-side
        if (!level.isClientSide) {
            PowerStoneLightningEntity beam = activeBeams.remove(uuid);
            if (beam != null && beam.isAlive()) beam.discard();
        }

        // Clear per-use state
        bedrockHitTicks.remove(uuid);
        lastBlockHit.remove(uuid);
        lastShotGameTime.remove(uuid);

        // Clear any lingering crack animation
        if (!level.isClientSide) {
            BlockPos prev = lastBlockHit.remove(uuid);
            if (prev != null) sendBlockBreakAnim(player, prev, -1);
        }

        // Apply centralized cooldown
        StoneAbilityCooldowns.apply(player, stack, "power", this);
    }

    // === Core firing logic once per holdInterval ===
    private void fireOneInterval(Level level, Player player, UUID uuid) {
        PowerStoneLightningEntity beam = activeBeams.get(uuid);

        Vec3 eye = player.position().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3 look = player.getLookAngle();
        double range = 30.0D;
        double step = 0.5D;

        Vec3 rayHit = null;
        BlockPos thisHitBlock = null;
        boolean hitBlock = false;

        outer:
        for (double d = 0; d <= range; d += step) {
            Vec3 pos = eye.add(look.scale(d));
            BlockPos blockPos = BlockPos.containing(pos);
            BlockState blockState = level.getBlockState(blockPos);

            if (!blockState.isAir() && blockState.getBlock() != Blocks.WATER && blockState.getBlock() != Blocks.BUBBLE_COLUMN) {
                thisHitBlock = blockPos;
                hitBlock = true;
                rayHit = pos;

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ModParticles.POWER_STONE_EFFECT_TWO.get(),
                            rayHit.x, rayHit.y, rayHit.z, 6, 0.1, 0.1, 0.1, 0.02);
                }

                // Bedrock special handling
                if (blockState.getBlock() == Blocks.BEDROCK) {
                    int prev = bedrockHitTicks.getOrDefault(uuid, 0) + 1;
                    bedrockHitTicks.put(uuid, prev);

                    if (prev % 4 == 0 && level instanceof ServerLevel serverLevel) {
                        double soundRange = 16.0;
                        for (ServerPlayer sp : serverLevel.players()) {
                            if (sp.position().distanceTo(Vec3.atCenterOf(blockPos)) <= soundRange) {
                                level.playSound(null, blockPos, net.minecraft.sounds.SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.5F, 1.0F);
                            }
                        }
                    }
                    level.levelEvent(2001, blockPos, Block.getId(blockState));
                    if (prev >= BEDROCK_BREAK_TICKS) {
                        level.destroyBlock(blockPos, true, player);
                        bedrockHitTicks.remove(uuid);
                    }
                } else {
                    // Generic block “mining” over time
                    float resistance = blockState.getBlock().getExplosionResistance();
                    int minTicks = 1;
                    int maxTicks = 40;
                    int requiredTicks = Math.max(minTicks, Math.min(maxTicks, (int) (resistance * 1.5)));

                    int mined = miningTicks.getOrDefault(blockPos, 0) + 1;
                    if (blockPos.equals(lastBlockHit.getOrDefault(uuid, null))) {
                        miningTicks.put(blockPos, mined);
                    } else {
                        BlockPos prevBlock = lastBlockHit.get(uuid);
                        if (prevBlock != null) sendBlockBreakAnim(player, prevBlock, -1);
                        miningTicks.put(blockPos, 1);
                        mined = 1;
                    }
                    lastBlockHit.put(uuid, blockPos);

                    int progress = Math.min((int) ((float) mined / requiredTicks * 9.0f), 9);
                    sendBlockBreakAnim(player, blockPos, progress);

                    if (mined >= requiredTicks) {
                        level.destroyBlock(blockPos, true, player);
                        miningTicks.remove(blockPos);
                        lastBlockHit.remove(uuid);
                        sendBlockBreakAnim(player, blockPos, -1);
                    }
                }
                break;
            }

            // Entity hit
            for (Entity entity : level.getEntities(player, player.getBoundingBox().move(look.scale(d)).inflate(0.5D))) {
                if (entity instanceof LivingEntity && entity != player) {
                    entity.hurt(level.damageSources().playerAttack(player), 40.0F);
                    if (level instanceof ServerLevel serverLevel) {
                        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
                        serverLevel.sendParticles(ModParticles.POWER_STONE_EFFECT_TWO.get(),
                                entityPos.x, entityPos.y, entityPos.z, 6, 0.1, 0.1, 0.1, 0.02);
                    }
                    rayHit = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
                    break outer;
                }
            }
        }

        if (rayHit == null) rayHit = eye.add(look.scale(range));
        if (!hitBlock || (hitBlock && (thisHitBlock == null || level.getBlockState(thisHitBlock).getBlock() != Blocks.BEDROCK))) {
            bedrockHitTicks.remove(uuid);
        }
        if (!hitBlock) {
            BlockPos prev = lastBlockHit.remove(uuid);
            if (prev != null) {
                miningTicks.remove(prev);
                sendBlockBreakAnim(player, prev, -1);
            }
        }

        updateBeam(level, player, uuid, eye, look, rayHit);
    }

    // Keep / update the beam even on skipped intervals (visual smoothness)
    private void updateBeamOnly(Level level, Player player, UUID uuid) {
        Vec3 eye = player.position().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3 look = player.getLookAngle();
        Vec3 rayHit = eye.add(look.scale(30.0D));
        updateBeam(level, player, uuid, eye, look, rayHit);
    }

    private void updateBeam(Level level, Player player, UUID uuid, Vec3 eye, Vec3 look, Vec3 hit) {
        Vec3 chest = player.position().add(0, 1.2, 0);
        Vec3 renderStart = chest.add(look.scale(0.2));
        Vec3 renderEnd = hit;

        double toHit = renderStart.distanceTo(hit);
        double toEye = renderStart.distanceTo(eye);
        if (toHit < 0.1 || toHit < toEye) {
            renderEnd = renderStart;
        }

        PowerStoneLightningEntity beam = activeBeams.get(uuid);
        if (beam == null || !beam.isAlive()) {
            beam = new PowerStoneLightningEntity(ModEntities.POWER_STONE_LIGHTNING.get(), level);
            level.addFreshEntity(beam);
            activeBeams.put(uuid, beam);
        }
        beam.setEndpoints(renderStart, renderEnd);

        if (level instanceof ServerLevel serverLevel) {
            Vec3 beamVec = renderEnd.subtract(renderStart);
            double beamLength = beamVec.length();
            Vec3 direction = beamVec.normalize();
            int sparkPoints = (int) (beamLength * 3.0);
            net.minecraft.util.RandomSource rand = serverLevel.random;
            for (int i = 0; i < sparkPoints; i++) {
                double t = i / (double) sparkPoints;
                Vec3 base = renderStart.add(direction.scale(t * beamLength));
                Vec3 randomPerp = direction.cross(new Vec3(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5)).normalize();
                double sparkLen = 0.4 + rand.nextDouble() * 0.3;
                Vec3 sparkTarget = base.add(randomPerp.scale(sparkLen));
                Vec3 velocity = sparkTarget.subtract(base).scale(0.3 + rand.nextDouble() * 0.3);
                serverLevel.sendParticles(ModParticles.POWER_STONE_EFFECT_ONE.get(),
                        base.x, base.y, base.z, 1, velocity.x, velocity.y, velocity.z, 0.0);
            }
        }
    }

    private void sendBlockBreakAnim(Player player, BlockPos pos, int progress) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundBlockDestructionPacket(sp.getId(), pos, progress));
        }
    }

}
