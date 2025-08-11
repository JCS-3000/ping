package org.jcs.egm.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jcs.egm.registry.ModParticles;

import java.util.Map;
import java.util.UUID;

/**
 * Logical “beacon-style” time field:
 * - Accelerates random ticks in a radius.
 * - Ages up babies inside the radius.
 * - Slowly converts brick blocks to their cracked variants.
 * - Renders lingering TSC particles client-side for the duration.
 */
public class TimeBubbleFieldEntity extends Entity {

    // Synced so client can render correct size/colors/duration
    private static final EntityDataAccessor<Integer> DATA_RADIUS  =
            SynchedEntityData.defineId(TimeBubbleFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TICKS   =
            SynchedEntityData.defineId(TimeBubbleFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR_A =
            SynchedEntityData.defineId(TimeBubbleFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR_B =
            SynchedEntityData.defineId(TimeBubbleFieldEntity.class, EntityDataSerializers.INT);

    private int targetTickSpeed = 900; // server workload
    private UUID owner;                // optional

    // Brick → cracked mappings (vanilla set; extend as you like)
    private static final Map<Block, Block> CRACK_MAP = Map.ofEntries(
            Map.entry(Blocks.STONE_BRICKS,                 Blocks.CRACKED_STONE_BRICKS),
            Map.entry(Blocks.DEEPSLATE_BRICKS,            Blocks.CRACKED_DEEPSLATE_BRICKS),
            Map.entry(Blocks.DEEPSLATE_TILES,             Blocks.CRACKED_DEEPSLATE_TILES),
            Map.entry(Blocks.NETHER_BRICKS,               Blocks.CRACKED_NETHER_BRICKS),
            Map.entry(Blocks.POLISHED_BLACKSTONE_BRICKS,  Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)
            // Add your modded bricks here if desired
    );

    // Probability a sampled brick converts this tick (with lots of samples, keep this low)
    private static final float PROB_CRACK = 0.008f; // ~0.8% per sample

    // Baby aging gain per tick (in vanilla, age < 0 means baby; moving toward 0 makes them older)
    private static final int BABY_AGE_GAIN_PER_TICK = 200; // 10s worth per game tick

    public TimeBubbleFieldEntity(EntityType<? extends TimeBubbleFieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setInvisible(true); // logical-only; visuals are particles
    }

    // Configure on server before addFreshEntity
    public TimeBubbleFieldEntity configure(UUID owner, int radius, int targetTickSpeed, int durationTicks, int colorA, int colorB) {
        this.owner = owner;
        this.targetTickSpeed = targetTickSpeed;
        this.entityData.set(DATA_RADIUS, radius);
        this.entityData.set(DATA_TICKS, durationTicks);
        this.entityData.set(DATA_COLOR_A, colorA);
        this.entityData.set(DATA_COLOR_B, colorB);
        return this;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 16);
        this.entityData.define(DATA_TICKS, 20 * 60);
        this.entityData.define(DATA_COLOR_A, 0x62FF2D);
        this.entityData.define(DATA_COLOR_B, 0x0AAA67);
    }

    @Override
    public void tick() {
        super.tick();

        int ticksRemaining = this.entityData.get(DATA_TICKS);
        if (ticksRemaining <= 0) {
            discard();
            return;
        }

        if (!level().isClientSide) {
            // ---------- SERVER ----------
            this.entityData.set(DATA_TICKS, ticksRemaining - 1);

            ServerLevel sl = (ServerLevel) level();
            final RandomSource r = sl.random;
            final int R = this.entityData.get(DATA_RADIUS);
            final int SAMPLES = targetTickSpeed; // heavy; tune if needed
            final int cx = Mth.floor(getX());
            final int cy = Mth.floor(getY());
            final int cz = Mth.floor(getZ());

            final int minY = Mth.clamp(cy - R, sl.getMinBuildHeight() + 1, sl.getMaxBuildHeight() - 2);
            final int maxY = Mth.clamp(cy + R, sl.getMinBuildHeight() + 1, sl.getMaxBuildHeight() - 2);

            // 1) Accelerate random ticks + crack bricks gradually
            for (int i = 0; i < SAMPLES; i++) {
                int x = cx + r.nextInt(R * 2 + 1) - R;
                int z = cz + r.nextInt(R * 2 + 1) - R;
                int y = Mth.clamp(cy + r.nextInt(R * 2 + 1) - R, minY, maxY);

                var bp = new net.minecraft.core.BlockPos(x, y, z);

                BlockState bs = sl.getBlockState(bp);
                if (bs.isRandomlyTicking()) {
                    bs.randomTick(sl, bp, r);
                }
                // Brick cracking
                Block cracked = CRACK_MAP.get(bs.getBlock());
                if (cracked != null && cracked != bs.getBlock() && r.nextFloat() < PROB_CRACK) {
                    sl.setBlock(bp, cracked.defaultBlockState(), 3);
                }

                FluidState fs = sl.getFluidState(bp);
                if (fs.isRandomlyTicking()) {
                    fs.randomTick(sl, bp, r);
                }
            }

            // 2) Age up babies inside the radius (fast-forward their growth)
            //    We do this every tick; the amount per tick is controlled by BABY_AGE_GAIN_PER_TICK.
            AABB box = new AABB(cx - R, cy - R, cz - R, cx + R, cy + R, cz + R);
            for (AgeableMob mob : sl.getEntitiesOfClass(AgeableMob.class, box, AgeableMob::isBaby)) {
                // In vanilla, getAge() < 0 means baby; moving toward 0 ages them up.
                int age = mob.getAge(); // typically negative
                int newAge = age + BABY_AGE_GAIN_PER_TICK;
                if (newAge >= 0) {
                    // Reached adulthood
                    mob.setAge(0);
                } else {
                    mob.setAge(newAge);
                }
            }

        } else {
            // ---------- CLIENT (lingering “potion-like” area) ----------
            final int R = this.entityData.get(DATA_RADIUS);
            final double yBase = getY() + 0.15;
            final int colorA = this.entityData.get(DATA_COLOR_A);
            final int colorB = this.entityData.get(DATA_COLOR_B);

            // Edge ring (alternating TSC)
            final int ringPts = 64;
            final double edgeR = R + 0.25;
            for (int i = 0; i < ringPts; i++) {
                double a = (2 * Math.PI * i / ringPts);
                double px = getX() + Math.cos(a) * edgeR;
                double pz = getZ() + Math.sin(a) * edgeR;
                float[] c = rgb01((i & 1) == 0 ? colorA : colorB);
                level().addParticle(ModParticles.UNIVERSAL_PARTICLE_ONE.get(), px, yBase, pz, c[0], c[1], c[2]);
            }

            // Light “steam” inside (small amounts of universal_particle_four float up)
            final int puffs = 18;
            for (int i = 0; i < puffs; i++) {
                double rx = (level().random.nextDouble() * 2 - 1) * R;
                double rz = (level().random.nextDouble() * 2 - 1) * R;
                if (rx * rx + rz * rz > R * R) continue;
                double px = getX() + rx;
                double pz = getZ() + rz;
                double py = yBase + level().random.nextDouble() * 0.35;

                float[] c = rgb01((i & 1) == 0 ? colorA : colorB);
                level().addParticle(ModParticles.UNIVERSAL_PARTICLE_FOUR.get(), px, py, pz, c[0], c[1], c[2]);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("radius", this.entityData.get(DATA_RADIUS));
        tag.putInt("ticks",  this.entityData.get(DATA_TICKS));
        tag.putInt("ca",     this.entityData.get(DATA_COLOR_A));
        tag.putInt("cb",     this.entityData.get(DATA_COLOR_B));
        tag.putInt("speed",  this.targetTickSpeed);
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_RADIUS, tag.getInt("radius"));
        this.entityData.set(DATA_TICKS,  tag.getInt("ticks"));
        this.entityData.set(DATA_COLOR_A, tag.getInt("ca"));
        this.entityData.set(DATA_COLOR_B, tag.getInt("cb"));
        this.targetTickSpeed = tag.getInt("speed");
        if (tag.hasUUID("owner")) this.owner = tag.getUUID("owner");
    }

    private static float[] rgb01(int hex) {
        return new float[] {
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >>  8) & 0xFF) / 255f,
                ( hex        & 0xFF) / 255f
        };
    }
}
