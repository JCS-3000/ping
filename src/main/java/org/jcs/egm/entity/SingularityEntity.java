package org.jcs.egm.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.registry.ModParticles;

import java.util.List;
import java.util.UUID;

public class SingularityEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_PULL_RADIUS =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_CASTER_UUID =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.STRING);

    private static final int DURATION_TICKS = 1000;
    private static final double PULL_RADIUS = 20.0;
    private static final double DAMAGE_RADIUS = 15.0;
    private static final double DESTROY_ITEM_RADIUS = 2.0;
    private static final float DAMAGE_AMOUNT = 2.0F;

    private UUID casterId;

    public SingularityEntity(EntityType<? extends SingularityEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public SingularityEntity(EntityType<? extends SingularityEntity> type, Level level, Vec3 position, UUID casterId) {
        this(type, level);
        this.setPos(position.x, position.y, position.z);
        this.casterId = casterId;
        this.entityData.set(DATA_CASTER_UUID, casterId.toString());
        this.entityData.set(DATA_PULL_RADIUS, (float) PULL_RADIUS);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AGE, 0);
        this.entityData.define(DATA_PULL_RADIUS, (float) PULL_RADIUS);
        this.entityData.define(DATA_CASTER_UUID, "");
    }

    @Override
    public void tick() {
        super.tick();

        int age = this.entityData.get(DATA_AGE);
        age++;
        this.entityData.set(DATA_AGE, age);

        if (age >= DURATION_TICKS) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            tickServerEffects(age);
        }
    }

    private void tickServerEffects(int age) {
        ServerLevel serverLevel = (ServerLevel) this.level();
        Vec3 center = this.position();

        if (age % 5 == 0) {
            applySingularityForces(serverLevel, center);
        }

        if (age % 2 == 0) {
            spawnSingularityParticles(serverLevel, center, age);
        }

        if (age % 20 == 0) {
            SoundEvent sound = SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath("egm", "singularity"));
            this.level().playSound(null, center.x, center.y, center.z, sound,
                    SoundSource.AMBIENT, 0.7F, 0.6F + (float)(age / (double)DURATION_TICKS) * 0.4F);
        }
    }

    private void applySingularityForces(ServerLevel level, Vec3 center) {
        AABB pullBox = new AABB(center.subtract(PULL_RADIUS, PULL_RADIUS, PULL_RADIUS),
                center.add(PULL_RADIUS, PULL_RADIUS, PULL_RADIUS));

        List<Entity> entities = level.getEntitiesOfClass(Entity.class, pullBox);

        for (Entity entity : entities) {
            if (entity == this) continue;
            
            // Skip caster (immunity)
            if (entity.getUUID().equals(this.casterId)) continue;

            double distance = entity.position().distanceTo(center);
            if (distance > PULL_RADIUS) continue;

            double pullStrength = Math.max(0, 1.0 - (distance / PULL_RADIUS));
            pullStrength = pullStrength * pullStrength;

            Vec3 toCenter = center.subtract(entity.position()).normalize();
            Vec3 pullForce = toCenter.scale(pullStrength * 1.1); // PULL STRENGTH

            entity.setDeltaMovement(entity.getDeltaMovement().add(pullForce));
            entity.hurtMarked = true;

            if (entity instanceof ItemEntity itemEntity && distance < DESTROY_ITEM_RADIUS) {
                for (int i = 0; i < 5; i++) {
                    NetworkHandler.sendTintedParticle(level, ModParticles.UNIVERSAL_PARTICLE_FOUR.get(),
                            entity.getX(), entity.getY(), entity.getZ(), 0.2f, 0.3f, 0.7f);
                }
                itemEntity.discard();
                continue;
            }

            if (entity instanceof LivingEntity living && distance < DAMAGE_RADIUS) {
                living.hurt(level.damageSources().magic(), DAMAGE_AMOUNT);
                for (int i = 0; i < 3; i++) {
                    NetworkHandler.sendTintedParticle(level, ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                            entity.getX(), entity.getY() + 1, entity.getZ(), 0.1f, 0.4f, 0.9f);
                }
            }
        }
    }

    private void spawnSingularityParticles(ServerLevel level, Vec3 center, int age) {
        // Flowing toward center - increased count for more density
        for (int i = 0; i < 20; i++) {
            double spiralTime = (age + i * 8) * 0.05;
            double radius = 6.0 - (spiralTime % 6.0);
            double angle = spiralTime * 3.5;
            double height = Math.sin(spiralTime * 2.5) * 1.8;
            if (radius < 0.3) continue;

            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + height;
            double z = center.z + Math.sin(angle) * radius;

            float intensity = (float)(1.0 - radius / 6.0);
            NetworkHandler.sendTintedParticle(level, ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                    x, y, z, 0.0f, 0.1f + intensity * 0.3f, 0.4f + intensity * 0.5f);
        }

        // Enhanced accretion disk with more particles
        double time = age * 0.1;
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * Math.PI * 2 + time;
            double radius = (2.0 + Math.sin(time + i) * 0.5) * 0.75;

            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + Math.sin(time * 2 + i) * 0.2;
            double z = center.z + Math.sin(angle) * radius;

            // Blue accretion disk - more intense
            NetworkHandler.sendTintedParticle(level, ModParticles.UNIVERSAL_PARTICLE_TWO.get(),
                    x, center.y, z, 0.15f, 0.25f, 0.6f);

            // Vertical swirl particles
            NetworkHandler.sendTintedParticle(level, ModParticles.UNIVERSAL_PARTICLE_THREE.get(),
                    x, y, z, 0.05f, 0.15f, 0.3f);
        }

        // Cosmic end rod particles - create streaming effect toward center
        if (age % 4 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * Math.PI * 2 + (age * 0.02);
                double outerRadius = 8.0 + Math.sin(age * 0.05 + i) * 1.5;
                
                double startX = center.x + Math.cos(angle) * outerRadius;
                double startY = center.y + Math.sin(age * 0.03 + i * 2) * 2.5;
                double startZ = center.z + Math.sin(angle) * outerRadius;
                
                // Create particle stream from outer ring to center
                spawnParticleStream(level, ParticleTypes.END_ROD, 
                    new Vec3(startX, startY, startZ), center, 6, 0.0);
            }
        }

        // Portal particles - create streams from mid-range to center
        if (age % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = 4.0 + Math.random() * 4.0;
                double height = (Math.random() - 0.5) * 3.0;
                
                double startX = center.x + Math.cos(angle) * radius;
                double startY = center.y + height;
                double startZ = center.z + Math.sin(angle) * radius;
                
                // Create particle stream from mid-range to center
                spawnParticleStream(level, ParticleTypes.PORTAL, 
                    new Vec3(startX, startY, startZ), center, 4, 0.05);
            }
        }

        // Dark smoke particles - create ominous streams flowing to center
        if (age % 2 == 0) {
            for (int i = 0; i < 4; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = 2.5 + Math.random() * 2.0;
                
                double startX = center.x + Math.cos(angle) * radius;
                double startY = center.y + (Math.random() - 0.5) * 1.0;
                double startZ = center.z + Math.sin(angle) * radius;
                
                // Create smoke stream flowing to center
                spawnParticleStream(level, ParticleTypes.LARGE_SMOKE, 
                    new Vec3(startX, startY, startZ), center, 3, 0.1);
            }
        }
    }

    /**
     * Creates a visual stream of particles from start point to end point
     */
    private void spawnParticleStream(ServerLevel level, net.minecraft.core.particles.ParticleOptions particleType, 
                                   Vec3 start, Vec3 end, int particleCount, double spread) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 step = direction.normalize().scale(distance / particleCount);
        
        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;
            Vec3 pos = start.add(step.scale(i));
            
            // Add slight randomness for more natural effect
            double offsetX = (Math.random() - 0.5) * spread;
            double offsetY = (Math.random() - 0.5) * spread;
            double offsetZ = (Math.random() - 0.5) * spread;
            
            // Spawn particle with no velocity (static positioning)
            level.sendParticles(particleType, 
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, 
                1, 0, 0, 0, 0);
        }
    }

    public int getAge() { return this.entityData.get(DATA_AGE); }
    public float getPullRadius() { return this.entityData.get(DATA_PULL_RADIUS); }

    public UUID getCasterId() {
        String uuidStr = this.entityData.get(DATA_CASTER_UUID);
        if (uuidStr.isEmpty()) return null;
        try { return UUID.fromString(uuidStr); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("CasterId")) {
            this.casterId = tag.getUUID("CasterId");
            this.entityData.set(DATA_CASTER_UUID, this.casterId.toString());
        }
        this.entityData.set(DATA_AGE, tag.getInt("Age"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.casterId != null) tag.putUUID("CasterId", this.casterId);
        tag.putInt("Age", this.entityData.get(DATA_AGE));
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource src, float amt) { return false; }
}
