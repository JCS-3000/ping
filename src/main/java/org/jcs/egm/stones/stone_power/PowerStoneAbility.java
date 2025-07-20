package org.jcs.egm.stones.stone_power;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneUseDamage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PowerStoneAbility implements IGStoneAbility {

    private final Map<UUID, Integer> bedrockHitTicks = new HashMap<>();
    private final Map<BlockPos, Integer> miningTicks = new HashMap<>();
    private final Map<UUID, BlockPos> lastBlockHit = new HashMap<>();

    private static final int BEDROCK_BREAK_TICKS = 120; // 6 seconds
    private static final int CHARGE_TICKS = 20;
    private static final int MAX_HAND_BEAM_TICKS = 60; // 3 seconds

    private final Map<UUID, Integer> handUseTicks = new HashMap<>();

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

        // Charging
        if (ticksHeld < CHARGE_TICKS) {
            if (level.isClientSide && ticksHeld % 4 == 0) {
                spawnChargeParticles(level, player);
            }
            if (!inGauntlet && !handUseTicks.containsKey(player.getUUID())) {
                handUseTicks.put(player.getUUID(), 0);
            }
            return;
        }

        // Hurt player if not in gauntlet
        if (!inGauntlet) {
            player.hurt(StoneUseDamage.get(level, player), 2.0F);
            int ticks = handUseTicks.getOrDefault(player.getUUID(), 0) + 1;
            handUseTicks.put(player.getUUID(), ticks);
            if (ticks >= MAX_HAND_BEAM_TICKS) {
                player.releaseUsingItem();
                return;
            }
        } else {
            handUseTicks.remove(player.getUUID());
        }

        // Beam logic -- start from same spot as Reality Stone
        Vec3 start = player.getEyePosition(1.0F).add(player.getLookAngle().scale(1.2));
        Vec3 look = player.getLookAngle();
        double range = 30.0D;
        Vec3 beamEnd = start.add(look.scale(range));

        // Use POWER_STONE_EFFECT_ONE particles for the beam
        spawnBeamParticles(level, start, beamEnd);

        if (!level.isClientSide) {
            double step = 0.5D;
            boolean hitBlock = false;
            BlockPos thisHitBlock = null;

            for (double d = 0; d <= range; d += step) {
                Vec3 pos = start.add(look.scale(d));
                BlockPos blockPos = BlockPos.containing(pos);
                BlockState blockState = level.getBlockState(blockPos);

                if (!blockState.isAir()) {
                    thisHitBlock = blockPos;

                    // === BEDROCK HANDLING ===
                    if (blockState.getBlock() == Blocks.BEDROCK) {
                        UUID playerId = player.getUUID();
                        int prev = bedrockHitTicks.getOrDefault(playerId, 0);
                        bedrockHitTicks.put(playerId, prev + 1);

                        // Play mining sound to everyone nearby every 4 ticks
                        if ((prev + 1) % 4 == 0 && level instanceof ServerLevel serverLevel) {
                            double soundRange = 16.0; // vanilla block break sound range
                            for (ServerPlayer sp : serverLevel.players()) {
                                if (sp.position().distanceTo(Vec3.atCenterOf(blockPos)) <= soundRange) {
                                    level.playSound(
                                            null, // everyone hears it
                                            blockPos,
                                            SoundEvents.STONE_HIT,
                                            SoundSource.BLOCKS,
                                            0.5F,
                                            1.0F
                                    );
                                }
                            }
                        }

                        // === BREAKING PARTICLES EACH TICK ===
                        if (!level.isClientSide) {
                            level.levelEvent(2001, blockPos, Block.getId(blockState));
                        }

                        if (prev + 1 >= BEDROCK_BREAK_TICKS) {
                            level.destroyBlock(blockPos, true, player);
                            bedrockHitTicks.remove(playerId);
                        }
                        // No cracks for bedrock (vanilla doesn't show them)
                    } else {
                        // === NORMAL BLOCK MINING ===
                        float resistance = blockState.getBlock().getExplosionResistance();
                        int minTicks = 1;
                        int maxTicks = 40;
                        int requiredTicks = (int) (resistance * 1.5);
                        requiredTicks = Math.max(minTicks, Math.min(maxTicks, requiredTicks));

                        int mined = miningTicks.getOrDefault(blockPos, 0) + 1;

                        // Continue progress if still aiming at same block
                        if (blockPos.equals(lastBlockHit.getOrDefault(player.getUUID(), null))) {
                            miningTicks.put(blockPos, mined);
                        } else {
                            // New block target: start progress, clear last block's cracks
                            BlockPos prevBlock = lastBlockHit.get(player.getUUID());
                            if (prevBlock != null)
                                sendBlockBreakAnim(player, prevBlock, -1);
                            miningTicks.put(blockPos, 1);
                            mined = 1;
                        }
                        lastBlockHit.put(player.getUUID(), blockPos);

                        // Show breaking animation (cracks)
                        int progress = (int) ((float) mined / requiredTicks * 9.0f);
                        progress = Math.min(progress, 9);
                        sendBlockBreakAnim(player, blockPos, progress);

                        if (mined >= requiredTicks) {
                            level.destroyBlock(blockPos, true, player);
                            miningTicks.remove(blockPos);
                            lastBlockHit.remove(player.getUUID());
                            sendBlockBreakAnim(player, blockPos, -1);
                        }
                    }
                    hitBlock = true;
                    break;
                }

                // Damage entities in beam
                for (Entity entity : level.getEntities(player, player.getBoundingBox().move(look.scale(d)).inflate(0.5D))) {
                    if (entity instanceof LivingEntity && entity != player) {
                        entity.hurt(level.damageSources().playerAttack(player), 40.0F);
                        entity.push(look.x * 2, 0.2, look.z * 2);
                    }
                }
            }
            // Reset bedrock timer if not hitting bedrock
            if (!hitBlock || (hitBlock && thisHitBlock == null || level.getBlockState(thisHitBlock).getBlock() != Blocks.BEDROCK)) {
                bedrockHitTicks.remove(player.getUUID());
            }
            // Reset mining tick/cracks if no block hit this tick
            if (!hitBlock) {
                BlockPos prev = lastBlockHit.remove(player.getUUID());
                if (prev != null) {
                    miningTicks.remove(prev);
                    sendBlockBreakAnim(player, prev, -1);
                }
            }
        }
    }

    public void onStoppedUsing(Level level, Player player, ItemStack gauntletStack, int count) {
        bedrockHitTicks.remove(player.getUUID());
        handUseTicks.remove(player.getUUID());
        BlockPos prev = lastBlockHit.remove(player.getUUID());
        if (prev != null) {
            miningTicks.remove(prev);
            if (!level.isClientSide)
                sendBlockBreakAnim(player, prev, -1);
        }
    }

    // === SEND BREAK ANIM ===
    private void sendBlockBreakAnim(Player player, BlockPos pos, int progress) {
        if (player instanceof ServerPlayer sp)
            sp.connection.send(new ClientboundBlockDestructionPacket(sp.getId(), pos, progress));
    }

    // === PARTICLE HELPERS ===
    private void spawnChargeParticles(Level level, Player player) {
        Vec3 pos = player.getEyePosition(1.0F).add(player.getLookAngle().scale(0.5));
        level.addParticle(ModParticles.POWER_STONE_EFFECT_ONE.get(), pos.x, pos.y, pos.z, 0, 0.01, 0);
    }

    // === BEAM WITH POWER_STONE_EFFECT_ONE PARTICLE ===
    private void spawnBeamParticles(Level level, Vec3 from, Vec3 to) {
        if (!(level instanceof ServerLevel sl)) return;
        double dist = from.distanceTo(to);
        Vec3 dir = to.subtract(from).normalize();
        RandomSource rand = sl.getRandom();
        int particlesPerStep = 3; // thin beam

        for (double i = 0; i < dist; i += 0.33D) {
            Vec3 pos = from.add(dir.scale(i));
            sl.sendParticles(ModParticles.POWER_STONE_EFFECT_ONE.get(), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

            for (int j = 0; j < particlesPerStep; j++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = 0.07 + rand.nextDouble() * 0.07;

                Vec3 up = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                Vec3 right = dir.cross(up).normalize();
                Vec3 up2 = dir.cross(right).normalize();

                Vec3 offset = right.scale(Math.cos(angle) * radius).add(up2.scale(Math.sin(angle) * radius));
                Vec3 particlePos = pos.add(offset);

                sl.sendParticles(ModParticles.POWER_STONE_EFFECT_ONE.get(), particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            }
        }
    }
}
