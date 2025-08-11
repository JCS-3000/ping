package org.jcs.egm.stones.stone_reality;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.stones.StoneItem;
import org.jcs.egm.stones.StoneUseDamage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WilledChaosRealityStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "willed_chaos"; }

    private static final double MAX_RANGE = 50.0D;
    private static final SoundEvent BEAM_SOUND   = SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "reality_stone_firing"));
    private static final SoundEvent CHANGE_SOUND = SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "reality_stone_change"));

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        Item cdItem = StoneAbilityCooldowns.pickCooldownItem(player, stack);
        if (StoneAbilityCooldowns.isCooling(player, cdItem)) return;
        StoneAbilityCooldowns.apply(player, cdItem, "reality", abilityKey());

        if (!StoneItem.isInGauntlet(player, stack)) {
            player.hurt(StoneUseDamage.get(level, player), 4.0F);
        }

        level.playSound(null, player.blockPosition(), BEAM_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 start = eye;
        Vec3 end = start.add(look.scale(MAX_RANGE));

        EntityHitResult entityResult = getEntityHitResult(level, player, start, end);
        BlockHitResult blockResult = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        double entityDist = entityResult != null ? entityResult.getLocation().distanceTo(start) : Double.POSITIVE_INFINITY;
        double blockDist = blockResult != null ? blockResult.getLocation().distanceTo(start) : Double.POSITIVE_INFINITY;

        Vec3 impact = (entityDist < blockDist && entityResult != null) ? entityResult.getLocation() : blockResult.getLocation();

        spawnBeamParticles((ServerLevel) level, start, impact);

        boolean transformed = false;
        Vec3 soundPos = null;

        if (entityResult != null && entityDist <= MAX_RANGE && entityDist < blockDist) {
            Entity hitEntity = entityResult.getEntity();
            if (hitEntity instanceof LivingEntity living && !(hitEntity instanceof Player)) {
                ResourceLocation srcId = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
                boolean srcHard = WilledChaosBlacklistHelper.isHardcodedEntityBlacklisted(srcId);
                boolean srcCfg  = WilledChaosBlacklistHelper.isConfigEntityBlacklisted(srcId);
                if (!srcHard && !srcCfg) {
                    transformed = tryReplaceMob((ServerLevel) level, living, player);
                    soundPos = entityResult.getLocation();
                }
            }
        } else if (blockResult != null && blockDist <= MAX_RANGE) {
            BlockPos blockPos = blockResult.getBlockPos();
            BlockState oldState = level.getBlockState(blockPos);
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(oldState.getBlock());
            boolean hard = WilledChaosBlacklistHelper.isHardcodedBlacklisted(blockId);
            boolean config = WilledChaosBlacklistHelper.isConfigBlacklisted(blockId);
            if (!oldState.isAir() && !hard && !config && tryReplaceBlock((ServerLevel) level, blockPos, oldState, player)) {
                transformed = true;
                soundPos = blockResult.getLocation();
            }
        }

        if (transformed && soundPos != null) {
            level.playSound(null, soundPos.x, soundPos.y, soundPos.z, CHANGE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean canHoldUse() { return false; }

    private EntityHitResult getEntityHitResult(Level level, Player player, Vec3 start, Vec3 end) {
        EntityHitResult result = null;
        double closest = MAX_RANGE;
        List<Entity> list = level.getEntities(player,
                player.getBoundingBox()
                        .expandTowards(end.subtract(start))
                        .inflate(1.0D),
                e -> e instanceof LivingEntity && !(e instanceof Player)
        );

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

    private boolean tryReplaceMob(ServerLevel level, LivingEntity target, Player player) {
        // Candidate destination pool:
        //  - Must create a Mob
        //  - Not the same as the source type
        //  - NOT blacklisted as a destination (prevents transforming INTO blacklisted forms)
        List<EntityType<?>> available = ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(type -> type != target.getType())
                .filter(type -> {
                    // Exclude blacklisted destination entity types
                    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
                    if (id != null) {
                        if (WilledChaosBlacklistHelper.isHardcodedEntityBlacklisted(id)) return false;
                        if (WilledChaosBlacklistHelper.isConfigEntityBlacklisted(id)) return false;
                    }
                    // Must be a mob type we can spawn
                    try {
                        Entity e = type.create(level);
                        return e instanceof Mob;
                    } catch (Exception ex) {
                        return false;
                    }
                })
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
        // Candidate destination pool:
        //  - Full block collision
        //  - Not the same as the source
        //  - NOT blacklisted as a destination
        List<Block> available = ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> block != oldState.getBlock())
                .filter(block -> {
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                    if (id != null) {
                        if (WilledChaosBlacklistHelper.isHardcodedBlacklisted(id)) return false;
                        if (WilledChaosBlacklistHelper.isConfigBlacklisted(id)) return false;
                    }
                    return block.defaultBlockState().isCollisionShapeFullBlock(level, pos);
                })
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
