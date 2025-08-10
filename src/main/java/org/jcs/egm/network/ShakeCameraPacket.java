package org.jcs.egm.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jcs.egm.client.fx.ShockwaveShakeHandler;
import org.jcs.egm.network.NetworkHandler;

import java.util.function.Supplier;

public class ShakeCameraPacket {
    private final int durationTicks;
    private final float intensity;

    public ShakeCameraPacket(int durationTicks, float intensity) {
        this.durationTicks = durationTicks;
        this.intensity = intensity;
    }

    public static void encode(ShakeCameraPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.durationTicks);
        buf.writeFloat(msg.intensity);
    }

    public static ShakeCameraPacket decode(FriendlyByteBuf buf) {
        return new ShakeCameraPacket(buf.readVarInt(), buf.readFloat());
    }

    public static void handle(ShakeCameraPacket msg, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        c.enqueueWork(() -> ShockwaveShakeHandler.trigger(msg.durationTicks, msg.intensity));
        c.setPacketHandled(true);
    }

    /** Send to a specific player's client. */
    public static void send(ServerPlayer player, int durationTicks, float intensity) {
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ShakeCameraPacket(durationTicks, intensity)
        );
    }
}
