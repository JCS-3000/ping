package org.jcs.egm.stones.stone_reality;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.registry.ModParticles;
import java.util.List;
import java.util.stream.Collectors;
import org.jcs.egm.stones.StoneUseDamage;



public class RealityStoneAbility implements IGStoneAbility {
    private static final int COOLDOWN_SECONDS = 3; //  How long the cooldown is, in seconds
    private static final int COOLDOWN_TICKS = 20 * COOLDOWN_SECONDS;
    private static final double MAX_DISTANCE = 50.0D; // Distance
    private static final double PARTICLES_PER_BLOCK = 10.0D; // Particles Per Block


    @Override
    public void activate(Level level, Player player, ItemStack gauntletStack) {
        System.out.println("[RealityStoneAbility] activate() called!"); // DEBUG
        if (level.isClientSide) return;

        // --- Stone Self-Damage Logic (applies only if not in gauntlet) ---
        boolean hasGauntlet = false;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem ||
                off.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem) {
            hasGauntlet = true;
        }
        if (!hasGauntlet) {
            player.hurt(StoneUseDamage.get(level, player), 6.0F);
        }

        // 1) Cooldown
        if (player instanceof ServerPlayer serverPlayer) {
            if (main.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem) {
                serverPlayer.getCooldowns().addCooldown(main.getItem(), COOLDOWN_TICKS);
            } else if (off.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem) {
                serverPlayer.getCooldowns().addCooldown(off.getItem(), COOLDOWN_TICKS);
            } else {
                // Fallback: if used directly, set cooldown on the stone
                serverPlayer.getCooldowns().addCooldown(ModItems.REALITY_STONE.get(), COOLDOWN_TICKS);
            }
        }

            // 2) Compute a “hand” start point
            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 look = player.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

            double forwardOffset = 0.3;
            double rightOffset = 0.3;
            double downOffset = 0.3;

            Vec3 start = eyePos
                    .add(look.scale(forwardOffset))
                    .add(right.scale(rightOffset))
                    .subtract(0, downOffset, 0);

            Vec3 end = start.add(look.scale(MAX_DISTANCE));

            // 3) Raytrace block
            HitResult blockTrace = level.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            ));

            // 4) Raytrace mob (living entity)
            EntityHitResult mobTrace = ProjectileUtil.getEntityHitResult(
                    level,
                    player,
                    start, end,
                    player.getBoundingBox().expandTowards(look.scale(MAX_DISTANCE)),
                    e -> e instanceof net.minecraft.world.entity.LivingEntity
            );

            // 5) Decide which was hit first (mob or block)
            double distBlock = blockTrace.getType() == HitResult.Type.BLOCK
                    ? blockTrace.getLocation().distanceTo(start)
                    : Double.MAX_VALUE;
            double distMob = mobTrace != null
                    ? mobTrace.getLocation().distanceTo(start)
                    : Double.MAX_VALUE;

            boolean hitMob = distMob < distBlock;

            Vec3 hitPos = hitMob
                    ? mobTrace.getLocation()
                    : blockTrace.getLocation();
            double dist = start.distanceTo(hitPos);
            int steps = (int) (dist * PARTICLES_PER_BLOCK);

            // 6) Spawn server→client particles along the line
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x = start.x + (hitPos.x - start.x) * t;
                double y = start.y + (hitPos.y - start.y) * t;
                double z = start.z + (hitPos.z - start.z) * t;
                ((ServerLevel) level).sendParticles(
                        ModParticles.REALITY_TRAIL.get(),
                        x, y, z,
                        1,    // count
                        0.0D, 0.0D, 0.0D, 0.0D
                );
            }

            // 7) Do the replacement
            if (hitMob && mobTrace != null && mobTrace.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
                // Replace mob with a random (non-blacklisted) mob
                RegistryAccess regs = level.registryAccess();
                List<?> allMobs = regs.registryOrThrow(Registries.ENTITY_TYPE)
                        .stream()
                        .filter(et -> {
                            var cat = et.getCategory();
                            if (cat == null) return false;
                            if (cat == net.minecraft.world.entity.MobCategory.MISC) return false;
                            // add other exclusions as you wish
                            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(et);
                            return !RealityStoneBlacklistHelper.isHardcodedEntityBlacklisted(id)
                                    && !RealityStoneBlacklistHelper.isConfigEntityBlacklisted(id);
                        })

                        .collect(Collectors.toList());
                if (!allMobs.isEmpty()) {
                    RandomSource rand = level.getRandom();
                    var chosenType = allMobs.get(rand.nextInt(allMobs.size()));
                    if (chosenType instanceof net.minecraft.world.entity.EntityType<?> et) {
                        var entity = et.create(level);
                        if (entity != null) {
                            entity.moveTo(living.getX(), living.getY(), living.getZ(), living.getYRot(), living.getXRot());
                            level.addFreshEntity(entity);
                            ((ServerLevel) level).sendParticles(
                                    ModParticles.REALITY_TRAIL.get(),
                                    living.getX(),
                                    living.getY() + living.getBbHeight() / 2.0,
                                    living.getZ(),
                                    100,    // Number of particles
                                    0.4, 0.4, 0.4,   // Spread
                                    0.01   // Particle speed
                            );
                            living.discard();
                        }
                    }
                }
            } else if (blockTrace.getType() == HitResult.Type.BLOCK) {
                BlockHitResult bhr = (BlockHitResult) blockTrace;
                BlockPos pos = bhr.getBlockPos();
                RegistryAccess regs = level.registryAccess();
                List<?> allBlocks = regs.registryOrThrow(Registries.BLOCK)
                        .stream()
                        .filter(b -> !RealityStoneBlacklistHelper.isHardcodedBlacklisted(ForgeRegistries.BLOCKS.getKey(b)))
                        .filter(b -> !RealityStoneBlacklistHelper.isConfigBlacklisted(ForgeRegistries.BLOCKS.getKey(b)))
                        .collect(Collectors.toList());
                if (!allBlocks.isEmpty()) {
                    RandomSource rand = level.getRandom();
                    var chosen = allBlocks.get(rand.nextInt(allBlocks.size()));
                    level.setBlockAndUpdate(pos, ((net.minecraft.world.level.block.Block) chosen).defaultBlockState());
                    ((ServerLevel) level).sendParticles(
                            ModParticles.REALITY_TRAIL.get(),
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            100,    // Number of particles
                            0.4, 0.4, 0.4,   // Spread
                            0.01   // Particle speed
                    );
                }
            }
        }
    }

