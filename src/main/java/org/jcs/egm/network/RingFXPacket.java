package org.jcs.egm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.client.fx.WristRingRenderer;

import java.util.function.Supplier;

public class RingFXPacket {
    private final int entityId;
    private final int ticksHeld;
    private final int colorAHex;
    private final int colorBHex;

    public RingFXPacket(int entityId, int ticksHeld, int colorAHex, int colorBHex) {
        this.entityId = entityId;
        this.ticksHeld = ticksHeld;
        this.colorAHex = colorAHex;
        this.colorBHex = colorBHex;
    }

    public RingFXPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.ticksHeld = buf.readVarInt();
        this.colorAHex = buf.readInt();
        this.colorBHex = buf.readInt();
    }

    public static void encode(RingFXPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.ticksHeld);
        buf.writeInt(msg.colorAHex);
        buf.writeInt(msg.colorBHex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) return;
                    WristRingRenderer.push(entityId, ticksHeld, colorAHex, colorBHex);
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
