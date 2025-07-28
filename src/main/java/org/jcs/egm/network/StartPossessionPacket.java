package org.jcs.egm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jcs.egm.client.input.ClientPossessionTracker;
import org.jcs.egm.stones.stone_mind.MindStoneOverlay;

import java.util.function.Supplier;

public class StartPossessionPacket {
    private final int durationTicks;
    private final int entityId;  // entity ID of the possessed mob

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
        // --- KEY: Set tracker active ---
        ClientPossessionTracker.setPossessing(true);

        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        // Start overlay
        MindStoneOverlay.start(duration);

        // Switch camera locally to the mob
        var e = level.getEntity(entityId);
        if (e != null) {
            mc.setCameraEntity(e);
        }
    }
}
