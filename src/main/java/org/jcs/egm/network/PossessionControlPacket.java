package org.jcs.egm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PossessionControlPacket {
    public final float strafe;
    public final float forward;
    public final boolean jump;
    public final boolean attack;
    public final float yaw;
    public final float pitch;

    public PossessionControlPacket(float strafe, float forward, boolean jump, boolean attack, float yaw, float pitch) {
        this.strafe  = strafe;
        this.forward = forward;
        this.jump    = jump;
        this.attack  = attack;
        this.yaw     = yaw;
        this.pitch   = pitch;
    }

    public static void encode(PossessionControlPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.strafe);
        buf.writeFloat(msg.forward);
        buf.writeBoolean(msg.jump);
        buf.writeBoolean(msg.attack);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
    }

    public static PossessionControlPacket decode(FriendlyByteBuf buf) {
        return new PossessionControlPacket(
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    org.jcs.egm.stones.stone_mind.EntityPossessionController
                            .receiveControlPacket(this, player);
                }
            });
        }
        ctx.setPacketHandled(true);
    }
}
