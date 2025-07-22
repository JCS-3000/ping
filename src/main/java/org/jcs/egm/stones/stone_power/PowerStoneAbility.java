package org.jcs.egm.stones.stone_power;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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

        // === CHARGE-UP PARTICLES ===
        if (ticksHeld < CHARGE_TICKS) {
            if (level.isClientSide && ticksHeld % 4 == 0) {
                spawnChargeParticles(level, player);
            }
            if (!inGauntlet && !handUseTicks.containsKey(player.getUUID())) {
                handUseTicks.put(player.getUUID(), 0);
            }
            return;
        }

        // === HAND USE LIMIT (if not in gauntlet) ===
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

        // === BEAM LOGIC AND BLOCK MINING ===
        Vec3 right = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(0.38);
        Vec3 forward = player.getLookAngle().scale(0.65);
        Vec3 beamStart = player.getEyePosition(1.0F)
                .add(right)
                .add(forward)
                .add(0, -0.45, 0);


        Vec3 look = player.getLookAngle();
        double range = 30.0D;
        double step = 0.5D;

        Vec3 beamVisualEnd = beamStart; // Default if nothing is hit
        BlockPos thisHitBlock = null;
        boolean hitBlock = false;
        Vec3 hitPos = null;

        if (!level.isClientSide) {
            for (double d = 0; d <= range; d += step) {
                Vec3 pos = beamStart.add(look.scale(d));
                beamVisualEnd = pos; // Always update

                BlockPos blockPos = BlockPos.containing(pos);
                BlockState blockState = level.getBlockState(blockPos);

                // === BLOCK HIT ===
                if (!blockState.isAir()) {
                    thisHitBlock = blockPos;
                    hitBlock = true;
                    hitPos = Vec3.atCenterOf(blockPos);

                    // --- Spawn impact effect (everyone sees it) ---
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ModParticles.POWER_STONE_EFFECT_TWO.get(),
                                hitPos.x, hitPos.y, hitPos.z,
                                6, 0.1, 0.1, 0.1, 0.02);
                    }

                    // === BEDROCK HANDLING ===
                    if (blockState.getBlock() == Blocks.BEDROCK) {
                        UUID playerId = player.getUUID();
                        int prev = bedrockHitTicks.getOrDefault(playerId, 0);
                        bedrockHitTicks.put(playerId, prev + 1);

                        if ((prev + 1) % 4 == 0 && level instanceof ServerLevel serverLevel) {
                            double soundRange = 16.0;
                            for (ServerPlayer sp : serverLevel.players()) {
                                if (sp.position().distanceTo(Vec3.atCenterOf(blockPos)) <= soundRange) {
                                    level.playSound(
                                            null,
                                            blockPos,
                                            SoundEvents.STONE_HIT,
                                            SoundSource.BLOCKS,
                                            0.5F,
                                            1.0F
                                    );
                                }
                            }
                        }

                        if (!level.isClientSide) {
                            level.levelEvent(2001, blockPos, Block.getId(blockState));
                        }

                        if (prev + 1 >= BEDROCK_BREAK_TICKS) {
                            level.destroyBlock(blockPos, true, player);
                            bedrockHitTicks.remove(playerId);
                        }
                        // No cracks for bedrock
                    } else {
                        // === NORMAL BLOCK MINING ===
                        float resistance = blockState.getBlock().getExplosionResistance();
                        int minTicks = 1;
                        int maxTicks = 40;
                        int requiredTicks = (int) (resistance * 1.5);
                        requiredTicks = Math.max(minTicks, Math.min(maxTicks, requiredTicks));

                        int mined = miningTicks.getOrDefault(blockPos, 0) + 1;

                        if (blockPos.equals(lastBlockHit.getOrDefault(player.getUUID(), null))) {
                            miningTicks.put(blockPos, mined);
                        } else {
                            BlockPos prevBlock = lastBlockHit.get(player.getUUID());
                            if (prevBlock != null)
                                sendBlockBreakAnim(player, prevBlock, -1);
                            miningTicks.put(blockPos, 1);
                            mined = 1;
                        }
                        lastBlockHit.put(player.getUUID(), blockPos);

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
                    break;
                }

                // === ENTITY HIT ===
                for (Entity entity : level.getEntities(player, player.getBoundingBox().move(look.scale(d)).inflate(0.5D))) {
                    if (entity instanceof LivingEntity && entity != player) {
                        entity.hurt(level.damageSources().playerAttack(player), 40.0F);
                        entity.push(look.x * 2, 0.2, look.z * 2);
                        // --- Spawn impact effect at entity ---
                        if (level instanceof ServerLevel serverLevel) {
                            Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
                            serverLevel.sendParticles(ModParticles.POWER_STONE_EFFECT_TWO.get(),
                                    entityPos.x, entityPos.y, entityPos.z,
                                    6, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }
            }
            // === RESET mining/breaking state if not hitting block ===
            if (!hitBlock || (hitBlock && thisHitBlock == null || level.getBlockState(thisHitBlock).getBlock() != Blocks.BEDROCK)) {
                bedrockHitTicks.remove(player.getUUID());
            }
            if (!hitBlock) {
                BlockPos prev = lastBlockHit.remove(player.getUUID());
                if (prev != null) {
                    miningTicks.remove(prev);
                    sendBlockBreakAnim(player, prev, -1);
                }
            }

            // === SUMMON LIGHTNING ARC ENTITY ===

            if (!beamVisualEnd.equals(beamStart)) {
                PowerStoneLightningEntity lightningArc = new PowerStoneLightningEntity(
                        ModEntities.POWER_STONE_LIGHTNING.get(),
                        (ServerLevel)level
                );
                lightningArc.setEndpoints(beamStart, beamVisualEnd);
                level.addFreshEntity(lightningArc);
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

    private void sendBlockBreakAnim(Player player, BlockPos pos, int progress) {
        if (player instanceof ServerPlayer sp)
            sp.connection.send(new ClientboundBlockDestructionPacket(sp.getId(), pos, progress));
    }

    private void spawnChargeParticles(Level level, Player player) {
        Vec3 pos = player.getEyePosition(1.0F).add(player.getLookAngle().scale(0.5));
        level.addParticle(ModParticles.POWER_STONE_EFFECT_ONE.get(), pos.x, pos.y, pos.z, 0, 0.01, 0);
    }
}
