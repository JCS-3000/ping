package org.jcs.egm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.client.PowerBeam;

import java.util.function.Supplier;

public class PowerBeamPacket {
    private final Vec3 from;
    private final Vec3 to;

    public PowerBeamPacket(Vec3 from, Vec3 to) {
        this.from = from;
        this.to = to;
    }

    public static void encode(PowerBeamPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.from.x);
        buf.writeDouble(msg.from.y);
        buf.writeDouble(msg.from.z);
        buf.writeDouble(msg.to.x);
        buf.writeDouble(msg.to.y);
        buf.writeDouble(msg.to.z);
    }

    public static PowerBeamPacket decode(FriendlyByteBuf buf) {
        Vec3 from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 to = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new PowerBeamPacket(from, to);
    }

    public static void handle(final PowerBeamPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Level level = Minecraft.getInstance().level;
                if (level != null) {
                    PowerBeam.spawn(level, msg.from, msg.to);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
