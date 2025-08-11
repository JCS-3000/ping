package org.jcs.egm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TintedParticlePacket {
    private final ResourceLocation particleId;
    private final double x, y, z;
    private final float r, g, b;

    public TintedParticlePacket(ResourceLocation particleId, double x, double y, double z, float r, float g, float b) {
        this.particleId = particleId; this.x = x; this.y = y; this.z = z; this.r = r; this.g = g; this.b = b;
    }

    public TintedParticlePacket(FriendlyByteBuf buf) {
        this.particleId = buf.readResourceLocation();
        this.x = buf.readDouble(); this.y = buf.readDouble(); this.z = buf.readDouble();
        this.r = buf.readFloat(); this.g = buf.readFloat(); this.b = buf.readFloat();
    }

    public static void encode(TintedParticlePacket pkt, FriendlyByteBuf buf) {
        buf.writeResourceLocation(pkt.particleId);
        buf.writeDouble(pkt.x); buf.writeDouble(pkt.y); buf.writeDouble(pkt.z);
        buf.writeFloat(pkt.r); buf.writeFloat(pkt.g); buf.writeFloat(pkt.b);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft mc = Minecraft.getInstance();
                    Level lvl = mc.level;
                    if (lvl == null) return;

                    ParticleType<?> t = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
                    if (t instanceof SimpleParticleType simple) {
                        // We pass RGB via speed components; your provider should read them as tint.
                        mc.level.addParticle(simple, x, y, z, r, g, b);
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
