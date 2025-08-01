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
import org.jcs.egm.registry.ModEntities;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.effects.StoneUseDamage;

import java.util.*;

public class InfiniteLightningPowerStoneAbility implements IGStoneAbility {

    private final Map<UUID, PowerStoneLightningEntity> activeBeams = new HashMap<>();
    private final Map<UUID, Integer> bedrockHitTicks = new HashMap<>();
    private final Map<BlockPos, Integer> miningTicks = new HashMap<>();
    private final Map<UUID, BlockPos> lastBlockHit = new HashMap<>();
    private final Map<UUID, Integer> handUseTicks = new HashMap<>();

    // Sound state tracking
    private final Set<UUID> chargingSoundPlayers = new HashSet<>();
    private final Set<UUID> firingSoundPlayers = new HashSet<>();
    private final Map<UUID, Integer> firingSoundStartTick = new HashMap<>();


    private static final int FIRING_SOUND_LENGTH_TICKS = 160; // For 8 seconds
    private static final SoundEvent CHARGING_SOUND = SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("egm", "power_stone_charging"));
    private static final SoundEvent FIRING_SOUND   = SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("egm", "power_stone_firing"));

    // Divide by 20 for seconds
    private static final int BEDROCK_BREAK_TICKS = 300;
    private static final int CHARGE_TICKS = 60;
    private static final int MAX_HAND_BEAM_TICKS = 60;

    private boolean isInGauntlet(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem ||
                off.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem;
    }

    @Override
    public void activate(Level level, Player player, ItemStack gauntletStack) {}

    @Override
    public boolean canHoldUse() { return true; }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack gauntletStack, int count) {
        int useDuration = player.getUseItem().getUseDuration();
        int ticksHeld = useDuration - count;
        boolean inGauntlet = isInGauntlet(player);

        UUID uuid = player.getUUID();

        // --- CHARGING PHASE ---
        if (ticksHeld < CHARGE_TICKS) {
            if (level.isClientSide && ticksHeld % 4 == 0) spawnChargeParticles(level, player);

            if (!inGauntlet && !handUseTicks.containsKey(uuid)) handUseTicks.put(uuid, 0);

            // ---- Play charging sound if not already playing ----
            if (level.isClientSide) {
                if (!chargingSoundPlayers.contains(uuid)) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            CHARGING_SOUND, SoundSource.PLAYERS,
                            0.8f, 1.0f, false);
                    chargingSoundPlayers.add(uuid);
                }
                // Make sure firing sound is NOT playing
                if (firingSoundPlayers.contains(uuid)) {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().getSoundManager().stop(FIRING_SOUND.getLocation(), SoundSource.PLAYERS);
                    }
                    firingSoundPlayers.remove(uuid);
                    firingSoundStartTick.remove(uuid);
                }
            }
            return;
        }

        // --- FIRING PHASE ---
        if (!inGauntlet) {
            player.hurt(StoneUseDamage.get(level, player), 2.0F);
            int ticks = handUseTicks.getOrDefault(uuid, 0) + 1;
            handUseTicks.put(uuid, ticks);
            if (ticks >= MAX_HAND_BEAM_TICKS) {
                player.releaseUsingItem();
                return;
            }
        } else {
            handUseTicks.remove(uuid);
        }

        if (level.isClientSide) {
            // Stop charging sound
            if (chargingSoundPlayers.contains(uuid)) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
                }
                chargingSoundPlayers.remove(uuid);
            }
            // Start or re-trigger firing sound
            int currentTick = Minecraft.getInstance().level != null ? (int)Minecraft.getInstance().level.getGameTime() : 0;
            Integer startedAt = firingSoundStartTick.get(uuid);
            boolean needsNewSound = !firingSoundPlayers.contains(uuid)
                    || startedAt == null
                    || (currentTick - startedAt) >= FIRING_SOUND_LENGTH_TICKS;
            if (needsNewSound) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        FIRING_SOUND, SoundSource.PLAYERS,
                        1.0f, 1.0f, true); // true = loop
                firingSoundPlayers.add(uuid);
                firingSoundStartTick.put(uuid, currentTick);
            }
        }

        if (level.isClientSide) return;

        PowerStoneLightningEntity beam = activeBeams.get(uuid);

        // === Correct Raytrace Origin: Player eye ===
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
                            rayHit.x, rayHit.y, rayHit.z,
                            6, 0.1, 0.1, 0.1, 0.02);
                }

                // Bedrock mining (unchanged)
                if (blockState.getBlock() == Blocks.BEDROCK) {
                    int prev = bedrockHitTicks.getOrDefault(uuid, 0);
                    bedrockHitTicks.put(uuid, prev + 1);

                    if ((prev + 1) % 4 == 0 && level instanceof ServerLevel serverLevel) {
                        double soundRange = 16.0;
                        for (ServerPlayer sp : serverLevel.players()) {
                            if (sp.position().distanceTo(Vec3.atCenterOf(blockPos)) <= soundRange) {
                                level.playSound(
                                        null, blockPos, net.minecraft.sounds.SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.5F, 1.0F
                                );
                            }
                        }
                    }

                    if (!level.isClientSide) {
                        level.levelEvent(2001, blockPos, Block.getId(blockState));
                    }

                    if (prev + 1 >= BEDROCK_BREAK_TICKS) {
                        level.destroyBlock(blockPos, true, player);
                        bedrockHitTicks.remove(uuid);
                    }
                } else {
                    // Standard block mining (unchanged)
                    float resistance = blockState.getBlock().getExplosionResistance();
                    int minTicks = 1;
                    int maxTicks = 40;
                    int requiredTicks = (int) (resistance * 1.5);
                    requiredTicks = Math.max(minTicks, Math.min(maxTicks, requiredTicks));

                    int mined = miningTicks.getOrDefault(blockPos, 0) + 1;
                    if (blockPos.equals(lastBlockHit.getOrDefault(uuid, null))) {
                        miningTicks.put(blockPos, mined);
                    } else {
                        BlockPos prevBlock = lastBlockHit.get(uuid);
                        if (prevBlock != null)
                            sendBlockBreakAnim(player, prevBlock, -1);
                        miningTicks.put(blockPos, 1);
                        mined = 1;
                    }
                    lastBlockHit.put(uuid, blockPos);

                    int progress = (int) ((float) mined / requiredTicks * 9.0f);
                    progress = Math.min(progress, 9);
                    sendBlockBreakAnim(player, blockPos, progress);

                    if (mined >= requiredTicks) {
                        level.destroyBlock(blockPos, true, player);
                        miningTicks.remove(blockPos);
                        lastBlockHit.remove(uuid);
                        sendBlockBreakAnim(player, blockPos, -1);
                    }
                }
                break outer;
            }

            // Entity hit (unchanged)
            for (Entity entity : level.getEntities(player, player.getBoundingBox().move(look.scale(d)).inflate(0.5D))) {
                if (entity instanceof LivingEntity && entity != player) {
                    entity.hurt(level.damageSources().playerAttack(player), 40.0F);
                    if (level instanceof ServerLevel serverLevel) {
                        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
                        serverLevel.sendParticles(ModParticles.POWER_STONE_EFFECT_TWO.get(),
                                entityPos.x, entityPos.y, entityPos.z,
                                6, 0.1, 0.1, 0.1, 0.02);
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

        // --- Now compute the visible beam: from chest/hand to hit ---
        Vec3 chest = player.position().add(0, 1.0, 0); // chest center
        Vec3 renderStart = chest.add(look.scale(0.2)); // adjust for hand if you wish
        Vec3 renderEnd = rayHit;

        double toHit = renderStart.distanceTo(rayHit);
        double toEye = renderStart.distanceTo(eye);
        if (toHit < 0.1 || toHit < toEye) {
            renderEnd = renderStart;
        }

        // --- Create or update singleton beam entity ---
        beam = activeBeams.get(uuid);
        if (beam == null || !beam.isAlive()) {
            beam = new PowerStoneLightningEntity(ModEntities.POWER_STONE_LIGHTNING.get(), (ServerLevel) level);
            level.addFreshEntity(beam);
            activeBeams.put(uuid, beam);
        }
        beam.setEndpoints(renderStart, renderEnd);

        // --- Electrical particles flying off the beam ---
        if (level instanceof ServerLevel serverLevel) {
            Vec3 beamVec = renderEnd.subtract(renderStart);
            double beamLength = beamVec.length();
            Vec3 direction = beamVec.normalize();
            int sparkPoints = (int) (beamLength * 3.0); // 7 sparks per block, tweak as desired
            net.minecraft.util.RandomSource rand = serverLevel.random;

            for (int i = 0; i < sparkPoints; i++) {
                double t = i / (double) sparkPoints;
                Vec3 base = renderStart.add(direction.scale(t * beamLength));
                Vec3 randomPerp = direction.cross(new Vec3(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5)).normalize();
                double sparkLen = 0.4 + rand.nextDouble() * 0.3; // length of the spark
                Vec3 sparkTarget = base.add(randomPerp.scale(sparkLen));
                // Velocity outward
                Vec3 velocity = sparkTarget.subtract(base).scale(0.3 + rand.nextDouble() * 0.3);
                // Spawn the spark
                serverLevel.sendParticles(
                        ModParticles.POWER_STONE_EFFECT_ONE.get(),
                        base.x, base.y, base.z,
                        1,
                        velocity.x, velocity.y, velocity.z, 0.0
                );
            }
        }

    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack gauntletStack, int count) {
        UUID uuid = player.getUUID();

        // --- Stop all sounds on release ---
        if (level.isClientSide) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
                Minecraft.getInstance().getSoundManager().stop(FIRING_SOUND.getLocation(), SoundSource.PLAYERS);
            }
            chargingSoundPlayers.remove(uuid);
            firingSoundPlayers.remove(uuid);
            firingSoundStartTick.remove(uuid);
        }

        // --- Beam logic ---
        if (!level.isClientSide) {
            PowerStoneLightningEntity beam = activeBeams.remove(uuid);
            if (beam != null && beam.isAlive()) beam.discard();
        }

        bedrockHitTicks.remove(uuid);
        handUseTicks.remove(uuid);
        BlockPos prev = lastBlockHit.remove(uuid);
        if (prev != null) {
            miningTicks.remove(prev);
            if (!level.isClientSide)
                sendBlockBreakAnim(player, prev, -1);
        }
    }

    private void sendBlockBreakAnim(Player player, BlockPos pos, int progress) {
        if (player instanceof ServerPlayer sp)
            sp.connection.send(new ClientboundBlockDestructionPacket(sp.getId(), pos, progress));
    }

    private void spawnChargeParticles(Level level, Player player) {
        Vec3 gauntletPos = player.position().add(0, 1.0, 0); // Near chest/hand
        net.minecraft.util.RandomSource rand = level.getRandom();

        int numParticles = 14; // Number of inward-traveling particles per tick
        double outerRadius = 2.2; // How far out the power is pulled from

        for (int i = 0; i < numParticles; i++) {
            // Random point on a sphere
            double theta = rand.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * rand.nextDouble() - 1);
            double x = outerRadius * Math.sin(phi) * Math.cos(theta);
            double y = outerRadius * Math.sin(phi) * Math.sin(theta);
            double z = outerRadius * Math.cos(phi);

            Vec3 start = gauntletPos.add(x, y, z);

            // Direction vector: from start point toward the gauntlet
            Vec3 velocity = gauntletPos.subtract(start).normalize().scale(0.22 + rand.nextDouble() * 0.12);

            // Particle travels toward the gauntlet
            level.addParticle(ModParticles.POWER_STONE_EFFECT_TWO.get(),
                    start.x, start.y, start.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }
}
