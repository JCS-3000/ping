package org.jcs.egm.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

public class MeteorEntity extends Entity {

    private static final EntityDataAccessor<Boolean> DATA_IS_STONE =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DELAY_TICKS =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_CASTER_UUID =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.STRING);

    private Vec3 targetLocation;
    private boolean isStone;
    private int delayTicks;
    private int currentTicks = 0;
    private boolean hasStartedFalling = false;
    private UUID casterId;
    private RandomSource random;

    // Space stone colors
    private static final Vector3f DARK_BLUE = new Vector3f(9f/255f, 27f/255f, 147f/255f);
    private static final Vector3f DARKER_BLUE = new Vector3f(0f/255f, 13f/255f, 102f/255f);
    private static final Vector3f BRIGHT_BLUE = new Vector3f(0f/255f, 25f/255f, 1f);

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.random = level.getRandom();
    }

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level, Vec3 spawnPos, 
                       Vec3 targetLocation, boolean isStone, int delayTicks, UUID casterId) {
        this(type, level);
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.targetLocation = targetLocation;
        this.isStone = isStone;
        this.delayTicks = delayTicks;
        this.casterId = casterId;
        
        // Set synched data
        this.entityData.set(DATA_IS_STONE, isStone);
        this.entityData.set(DATA_DELAY_TICKS, delayTicks);
        this.entityData.set(DATA_TARGET_X, (float) targetLocation.x);
        this.entityData.set(DATA_TARGET_Y, (float) targetLocation.y);
        this.entityData.set(DATA_TARGET_Z, (float) targetLocation.z);
        this.entityData.set(DATA_CASTER_UUID, casterId.toString());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_IS_STONE, true);
        this.entityData.define(DATA_DELAY_TICKS, 0);
        this.entityData.define(DATA_TARGET_X, 0.0F);
        this.entityData.define(DATA_TARGET_Y, 0.0F);
        this.entityData.define(DATA_TARGET_Z, 0.0F);
        this.entityData.define(DATA_CASTER_UUID, "");
    }

    @Override
    public void tick() {
        super.tick();
        
        currentTicks++;
        
        // Wait for delay before starting to fall
        if (currentTicks <= delayTicks) {
            // Hover in place, maybe with some particles
            if (!level().isClientSide && currentTicks % 10 == 0) {
                spawnWaitingParticles();
            }
            return;
        }
        
        if (!hasStartedFalling) {
            hasStartedFalling = true;
            // Initialize falling velocity
            this.setDeltaMovement(0, -0.3, 0);
        }
        
        // Update target location from synched data if needed
        if (targetLocation == null) {
            float targetX = this.entityData.get(DATA_TARGET_X);
            float targetY = this.entityData.get(DATA_TARGET_Y);
            float targetZ = this.entityData.get(DATA_TARGET_Z);
            targetLocation = new Vec3(targetX, targetY, targetZ);
            isStone = this.entityData.get(DATA_IS_STONE);
            delayTicks = this.entityData.get(DATA_DELAY_TICKS);
        }
        
        // Apply gravity and move toward target
        Vec3 velocity = this.getDeltaMovement();
        
        // Increase falling speed (gravity)
        velocity = velocity.add(0, -0.08, 0);
        
        // Slight homing toward target
        Vec3 toTarget = targetLocation.subtract(this.position()).normalize();
        velocity = velocity.add(toTarget.scale(0.02));
        
        this.setDeltaMovement(velocity);
        this.move(net.minecraft.world.entity.MoverType.SELF, velocity);
        
        // Spawn trail particles
        if (!level().isClientSide) {
            spawnTrailParticles();
        }
        
        // Check for impact
        if (this.position().y <= targetLocation.y + 1.0) {
            explode();
        }
        
        // Safety check - remove if too old
        if (currentTicks > 600) { // 30 seconds max
            this.discard();
        }
    }

    private void spawnWaitingParticles() {
        if (level() instanceof ServerLevel server) {
            // Gentle floating particles while waiting
            server.sendParticles(ParticleTypes.ENCHANT,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.3, 0.3, 0.3, 0.01);
        }
    }

    private void spawnTrailParticles() {
        if (level() instanceof ServerLevel server) {
            // Use vanilla dust particles with space stone colors
            DustParticleOptions darkDust = new DustParticleOptions(DARK_BLUE, 1.0f);
            DustParticleOptions darkerDust = new DustParticleOptions(DARKER_BLUE, 1.0f);
            DustParticleOptions brightDust = new DustParticleOptions(BRIGHT_BLUE, 1.0f);
            
            // Space stone dust particles
            server.sendParticles(brightDust,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.1, 0.1, 0.1, 0.02);
            
            server.sendParticles(darkDust,
                    this.getX() + (random.nextDouble() - 0.5) * 0.5, 
                    this.getY() + (random.nextDouble() - 0.5) * 0.5, 
                    this.getZ() + (random.nextDouble() - 0.5) * 0.5, 
                    1, 0.1, 0.1, 0.1, 0.02);
                    
            server.sendParticles(darkerDust,
                    this.getX() + (random.nextDouble() - 0.5) * 0.3, 
                    this.getY() + (random.nextDouble() - 0.5) * 0.3, 
                    this.getZ() + (random.nextDouble() - 0.5) * 0.3, 
                    1, 0.1, 0.1, 0.1, 0.02);
            
            // Fire particles
            server.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    4, 0.3, 0.3, 0.3, 0.02);
            
            // Smoke particles
            server.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.4, 0.4, 0.4, 0.01);
        }
    }

    private void explode() {
        if (level().isClientSide) return;
        
        // Create half TNT-equivalent explosion
        level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, 
                Level.ExplosionInteraction.TNT);
        
        // Play explosion sound
        level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, 
                SoundSource.HOSTILE, 2.0F, 1.0F);
        
        // Extra particle effects
        if (level() instanceof ServerLevel server) {
            // Large explosion particle burst
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0, 0, 0, 0);
            
            // Additional fire and smoke
            server.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    20, 2.0, 1.0, 2.0, 0.1);
                    
            server.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    15, 1.5, 1.0, 1.5, 0.05);
                    
            // Space stone colored dust explosion
            DustParticleOptions brightDust = new DustParticleOptions(BRIGHT_BLUE, 1.5f);
            server.sendParticles(brightDust,
                    this.getX(), this.getY(), this.getZ(),
                    10, 1.0, 1.0, 1.0, 0.1);
        }
        
        this.discard();
    }

    public boolean isStone() {
        return this.entityData.get(DATA_IS_STONE);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TargetX")) {
            targetLocation = new Vec3(
                tag.getDouble("TargetX"),
                tag.getDouble("TargetY"),
                tag.getDouble("TargetZ")
            );
        }
        isStone = tag.getBoolean("IsStone");
        delayTicks = tag.getInt("DelayTicks");
        currentTicks = tag.getInt("CurrentTicks");
        hasStartedFalling = tag.getBoolean("HasStartedFalling");
        if (tag.contains("CasterId")) {
            casterId = UUID.fromString(tag.getString("CasterId"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (targetLocation != null) {
            tag.putDouble("TargetX", targetLocation.x);
            tag.putDouble("TargetY", targetLocation.y);
            tag.putDouble("TargetZ", targetLocation.z);
        }
        tag.putBoolean("IsStone", isStone);
        tag.putInt("DelayTicks", delayTicks);
        tag.putInt("CurrentTicks", currentTicks);
        tag.putBoolean("HasStartedFalling", hasStartedFalling);
        if (casterId != null) {
            tag.putString("CasterId", casterId.toString());
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        return false;
    }
}