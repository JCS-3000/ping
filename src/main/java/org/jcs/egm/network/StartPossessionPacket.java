package org.jcs.egm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.stones.stone_mind.MindStoneOverlay;

import java.util.function.Supplier;

public class StartPossessionPacket {
    private final int durationTicks;
    private final int entityId;  // network entity ID, not UUID

    public StartPossessionPacket(int durationTicks, int entityId) {
        this.durationTicks = durationTicks;
        this.entityId = entityId;
    }

    public static void encode(StartPossessionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.durationTicks);
        buf.writeInt(msg.entityId);
    }

    public static StartPossessionPacket decode(FriendlyByteBuf buf) {
        return new StartPossessionPacket(
                buf.readInt(),
                buf.readInt()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupp) {
        NetworkEvent.Context ctx = ctxSupp.get();
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.enqueueWork(() -> handleClient(durationTicks, entityId));
        }
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(int duration, int entityId) {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        // 1) start the overlay
        MindStoneOverlay.start(duration);

        // 2) switch camera locally to the mob
        Entity e = level.getEntity(entityId);
        if (e != null) {
            mc.setCameraEntity(e);
        }
    }
}
