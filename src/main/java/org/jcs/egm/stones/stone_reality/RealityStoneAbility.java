package org.jcs.egm.stones.stone_reality;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneUseDamage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RealityStoneAbility implements IGStoneAbility {
    private static final int COOLDOWN_TICKS_HAND = 120; // 6 seconds
    private static final int COOLDOWN_TICKS_GAUNTLET = 40; // 2 seconds
    private static final double MAX_RANGE = 50.0D;

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        boolean inGauntlet = IGStoneAbility.isInGauntlet(player, stack);

        int cooldown = inGauntlet ? COOLDOWN_TICKS_GAUNTLET : COOLDOWN_TICKS_HAND;
        ItemStack cooldownStack = (inGauntlet && stack != null && stack.getItem().getClass().getSimpleName().equals("InfinityGauntletItem"))
                ? stack
                : player.getMainHandItem();

        // Silent cooldown check
        if (player.getCooldowns().isOnCooldown(cooldownStack.getItem())) {
            return;
        }

        player.getCooldowns().addCooldown(cooldownStack.getItem(), cooldown);

        if (!inGauntlet) {
            player.hurt(StoneUseDamage.get(level, player), 4.0F);
        }

        // Play activation sound
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.PLAYERS, 0.6F, 1.6F);

        // Raytrace setup
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 start = eye.add(look.scale(1.2));
        Vec3 end = start.add(look.scale(MAX_RANGE));

        EntityHitResult entityResult = getEntityHitResult(level, player, start, end);
        BlockHitResult blockResult = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        double entityDist = entityResult != null ? entityResult.getLocation().distanceTo(start) : Double.POSITIVE_INFINITY;
        double blockDist = blockResult != null ? blockResult.getLocation().distanceTo(start) : Double.POSITIVE_INFINITY;

        // Particle
        spawnBeamParticles((ServerLevel) level, start, (entityDist < blockDist ? entityResult.getLocation() : blockResult.getLocation()));

        boolean transformed = false;

        // Priority: Mob > Block
        if (entityResult != null && entityDist <= MAX_RANGE && entityDist < blockDist) {
            Entity hitEntity = entityResult.getEntity();
            if (hitEntity instanceof LivingEntity living && !(hitEntity instanceof Player)) {
                transformed = tryReplaceMob((ServerLevel) level, living, player);
            }
        } else if (blockResult != null && blockDist <= MAX_RANGE) {
            BlockPos blockPos = blockResult.getBlockPos();
            BlockState oldState = level.getBlockState(blockPos);
            if (!oldState.isAir() && tryReplaceBlock((ServerLevel) level, blockPos, oldState, player)) {
                transformed = true;
            }
        }

        // Play a transformation sound at the hit point if something changed
        if (transformed) {
            Vec3 soundPos = (entityDist < blockDist && entityResult != null) ? entityResult.getLocation() : blockResult.getLocation();
            level.playSound(null, soundPos.x, soundPos.y, soundPos.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.85F, 1.0F);
        }
    }

    @Override
    public boolean canHoldUse() { return false; }

    // (Helpers unchanged...)

    private EntityHitResult getEntityHitResult(Level level, Player player, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(1.0D);
        List<Entity> entities = level.getEntities(player, box, e -> e instanceof LivingEntity && !(e instanceof Player));
        EntityHitResult best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Entity entity : entities) {
            AABB entityBB = entity.getBoundingBox().inflate(0.25D);
            Optional<Vec3> intercept = entityBB.clip(start, end);
            if (intercept.isPresent()) {
                double dist = start.distanceTo(intercept.get());
                if (dist < bestDist && dist <= MAX_RANGE) {
                    bestDist = dist;
                    best = new EntityHitResult(entity, intercept.get());
                }
            }
        }
        return best;
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

    private boolean tryReplaceMob(ServerLevel level, LivingEntity target, Player player) {
        List<EntityType<?>> available = ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(type -> type != target.getType())
                .filter(type -> Mob.class.isAssignableFrom(type.getBaseClass()))
                .collect(Collectors.toList());
        if (available.isEmpty()) return false;
        RandomSource rand = level.getRandom();
        EntityType<?> pick = available.get(rand.nextInt(available.size()));
        Mob mob = (Mob) pick.create(level);
        if (mob == null) return false;
        mob.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        target.discard();
        level.addFreshEntity(mob);
        spawnBurstParticles(level, mob.position());
        return true;
    }

    private boolean tryReplaceBlock(ServerLevel level, BlockPos pos, BlockState oldState, Player player) {
        ResourceLocation oldId = ForgeRegistries.BLOCKS.getKey(oldState.getBlock());
        if (oldId != null && oldId.toString().equals("minecraft:bedrock")) {
            return false;
        }
        List<Block> available = ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> block != oldState.getBlock())
                .filter(block -> block.defaultBlockState().isCollisionShapeFullBlock(level, pos))
                .collect(Collectors.toList());
        if (available.isEmpty()) return false;
        RandomSource rand = level.getRandom();
        Block newBlock = available.get(rand.nextInt(available.size()));
        level.setBlockAndUpdate(pos, newBlock.defaultBlockState());
        spawnBurstParticles(level, Vec3.atCenterOf(pos));
        return true;
    }

    private void spawnBurstParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticles.REALITY_STONE_EFFECT_ONE.get(), pos.x, pos.y, pos.z, 100, 0.25, 0.25, 0.25, 0.01);
    }
}
