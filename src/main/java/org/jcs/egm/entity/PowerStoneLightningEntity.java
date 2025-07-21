package org.jcs.egm.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class PowerStoneLightningEntity extends Entity {
    private Vec3 start;
    private Vec3 end;
    private int life; // ticks left (for fade out, etc.)

    public PowerStoneLightningEntity(EntityType<?> type, Level world) {
        super(type, world);
        this.noPhysics = true;
        this.life = 10; // visible for 10 ticks (half a second)
    }

    public PowerStoneLightningEntity(EntityType<?> type, Level world, Vec3 start, Vec3 end) {
        this(type, world);
        this.start = start;
        this.end = end;
        setPos(start.x, start.y, start.z);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();
        if (--life <= 0) discard();
    }

    public Vec3 getStart() { return start; }
    public Vec3 getEnd() { return end; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
