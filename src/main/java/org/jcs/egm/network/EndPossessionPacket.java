package org.jcs.egm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jcs.egm.client.input.ClientPossessionTracker;
import org.jcs.egm.stones.stone_mind.MindStoneOverlay;

import java.util.function.Supplier;

public class EndPossessionPacket {
    public EndPossessionPacket() {}

    public static void encode(EndPossessionPacket msg, FriendlyByteBuf buf) {}

    public static EndPossessionPacket decode(FriendlyByteBuf buf) {
        return new EndPossessionPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupp) {
        NetworkEvent.Context ctx = ctxSupp.get();
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.enqueueWork(EndPossessionPacket::handleClient);
        }
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient() {
        // --- KEY: Set tracker inactive ---
        ClientPossessionTracker.setPossessing(false);

        Minecraft mc = Minecraft.getInstance();
        MindStoneOverlay.stop();
        // reset camera back to player
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
    }
}
