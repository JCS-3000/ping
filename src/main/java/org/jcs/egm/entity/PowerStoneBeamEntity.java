package org.jcs.egm.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.registry.ModEntities;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * PowerStoneBeamEntity - visual only, does not handle breaking blocks or damaging entities (handled in ability class).
 */
public class PowerStoneBeamEntity extends Entity {
    // Accessible by renderer, ability
    @Nullable
    protected Vec3 start;
    @Nullable
    protected Vec3 end;
    @Nullable
    protected UUID ownerUUID;

    // Failsafe: auto-despawn after 10 ticks if endpoints not set
    private int ticksWithoutEndpoints = 0;

    public PowerStoneBeamEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }

    public PowerStoneBeamEntity(Level level, Player owner) {
        super(ModEntities.POWER_STONE_BEAM.get(), level);
        this.ownerUUID = owner.getUUID();
    }

    public void setEndpoints(Vec3 start, Vec3 end) {
        this.start = start;
        this.end = end;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Vec3 getStart() {
        return start;
    }

    public Vec3 getEnd() {
        return end;
    }

    @Override
    public void tick() {
        super.tick();

        // Defensive null check for endpoints
        if (this.start == null || this.end == null) {
            ticksWithoutEndpoints++;
            if (ticksWithoutEndpoints > 10) {
                discard();
            }
            return;
        }

        // Client: spawn beam particles along the path (visual only)
        if (level().isClientSide && tickCount % 2 == 0) {
            double dist = start.distanceTo(end);
            int steps = Math.max(2, (int) (dist * 4));
            for (int i = 0; i < steps; i++) {
                double t = i / (double) (steps - 1);
                double x = Mth.lerp(t, start.x, end.x);
                double y = Mth.lerp(t, start.y, end.y);
                double z = Mth.lerp(t, start.z, end.z);
                level().addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
            }
        }

        // Auto-despawn after 50 ticks (2.5s at 20 TPS), adjust as needed
        if (tickCount > 50) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("StartX")) {
            this.start = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
        }
        if (tag.contains("EndX")) {
            this.end = new Vec3(tag.getDouble("EndX"), tag.getDouble("EndY"), tag.getDouble("EndZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        if (start != null) {
            tag.putDouble("StartX", start.x);
            tag.putDouble("StartY", start.y);
            tag.putDouble("StartZ", start.z);
        }
        if (end != null) {
            tag.putDouble("EndX", end.x);
            tag.putDouble("EndY", end.y);
            tag.putDouble("EndZ", end.z);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
