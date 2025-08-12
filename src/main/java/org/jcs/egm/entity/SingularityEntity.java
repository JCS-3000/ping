package org.jcs.egm.entity;

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

    private static final int DURATION_TICKS = 160;
    private static final double PULL_RADIUS = 12.0;
    private static final double DAMAGE_RADIUS = 3.0;
    private static final double DESTROY_ITEM_RADIUS = 2.0;
    private static final float DAMAGE_AMOUNT = 8.0F;

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

            double distance = entity.position().distanceTo(center);
            if (distance > PULL_RADIUS) continue;

            double pullStrength = Math.max(0, 1.0 - (distance / PULL_RADIUS));
            pullStrength = pullStrength * pullStrength;

            Vec3 toCenter = center.subtract(entity.position()).normalize();
            Vec3 pullForce = toCenter.scale(pullStrength * 0.3);

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
        // Flowing toward center
        for (int i = 0; i < 12; i++) {
            double spiralTime = (age + i * 10) * 0.05;
            double radius = 5.0 - (spiralTime % 5.0);
            double angle = spiralTime * 3.0;
            double height = Math.sin(spiralTime * 2) * 1.5;
            if (radius < 0.5) continue;

            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + height;
            double z = center.z + Math.sin(angle) * radius;

            float intensity = (float)(1.0 - radius / 5.0);
            NetworkHandler.sendTintedParticle(level, ModParticles.UNIVERSAL_PARTICLE_ONE.get(),
                    x, y, z, 0.0f, 0.1f + intensity * 0.2f, 0.3f + intensity * 0.4f);
        }

        // Swirling "outside blue" accretion disk — now 25% closer to the cube
        double time = age * 0.1;
        for (int i = 0; i < 6; i++) {
            double angle = (i / 6.0) * Math.PI * 2 + time;

            // ORIGINAL: radius = 2.0 + sin(time + i) * 0.5;
            // NEW: bring 25% closer -> multiply by 0.75
            double radius = (2.0 + Math.sin(time + i) * 0.5) * 0.75;

            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + Math.sin(time * 2 + i) * 0.2;
            double z = center.z + Math.sin(angle) * radius;

            // Blue accretion disk
            NetworkHandler.sendTintedParticle(level, ModParticles.UNIVERSAL_PARTICLE_TWO.get(),
                    x, center.y, z, 0.1f, 0.2f, 0.5f);

            // Keep (very faint) vertical swirl if you want; else comment out.
            NetworkHandler.sendTintedParticle(level, ModParticles.UNIVERSAL_PARTICLE_THREE.get(),
                    x, y, z, 0.0f, 0.1f, 0.2f);
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
