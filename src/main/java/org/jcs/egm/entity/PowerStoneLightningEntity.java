package org.jcs.egm.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class PowerStoneLightningEntity extends Entity {
    // Synced entity data for start/end points (use FLOAT for compatibility)
    public static final EntityDataAccessor<Float> START_X = SynchedEntityData.defineId(PowerStoneLightningEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> START_Y = SynchedEntityData.defineId(PowerStoneLightningEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> START_Z = SynchedEntityData.defineId(PowerStoneLightningEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(PowerStoneLightningEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(PowerStoneLightningEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(PowerStoneLightningEntity.class, EntityDataSerializers.FLOAT);

    // Track last tick endpoints were set (for instant despawn)
    private int lastUpdatedTick = 0;

    public PowerStoneLightningEntity(EntityType<?> type, Level world) {
        super(type, world);
        this.noPhysics = true;
    }

    // Call this immediately after construction and before addFreshEntity!
    public void setEndpoints(Vec3 start, Vec3 end) {
        if (start == null || end == null) return;
        this.entityData.set(START_X, (float)start.x);
        this.entityData.set(START_Y, (float)start.y);
        this.entityData.set(START_Z, (float)start.z);
        this.entityData.set(END_X, (float)end.x);
        this.entityData.set(END_Y, (float)end.y);
        this.entityData.set(END_Z, (float)end.z);
        setPos(start.x, start.y, start.z);
        lastUpdatedTick = this.tickCount; // Track updates for despawn
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(START_X, 0F);
        this.entityData.define(START_Y, 0F);
        this.entityData.define(START_Z, 0F);
        this.entityData.define(END_X, 0F);
        this.entityData.define(END_Y, 0F);
        this.entityData.define(END_Z, 0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setEndpoints(
                new Vec3(tag.getFloat("sx"), tag.getFloat("sy"), tag.getFloat("sz")),
                new Vec3(tag.getFloat("ex"), tag.getFloat("ey"), tag.getFloat("ez"))
        );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("sx", this.entityData.get(START_X));
        tag.putFloat("sy", this.entityData.get(START_Y));
        tag.putFloat("sz", this.entityData.get(START_Z));
        tag.putFloat("ex", this.entityData.get(END_X));
        tag.putFloat("ey", this.entityData.get(END_Y));
        tag.putFloat("ez", this.entityData.get(END_Z));
    }

    public Vec3 getStart() {
        return new Vec3(
                this.entityData.get(START_X),
                this.entityData.get(START_Y),
                this.entityData.get(START_Z)
        );
    }
    public Vec3 getEnd() {
        return new Vec3(
                this.entityData.get(END_X),
                this.entityData.get(END_Y),
                this.entityData.get(END_Z)
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // Despawn if not updated for 2 ticks (i.e., use released)
            if (this.tickCount - lastUpdatedTick > 2) {
                this.discard();
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
