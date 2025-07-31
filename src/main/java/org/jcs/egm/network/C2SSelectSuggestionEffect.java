package org.jcs.egm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.stones.stone_mind.MindStoneSuggestionEffect;

import java.util.function.Supplier;

public class C2SSelectSuggestionEffect {
    public final int selected;

    public C2SSelectSuggestionEffect(int sel) { this.selected = sel; }

    public static void encode(C2SSelectSuggestionEffect msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.selected);
    }

    public static C2SSelectSuggestionEffect decode(FriendlyByteBuf buf) {
        return new C2SSelectSuggestionEffect(buf.readVarInt());
    }

    public static void handle(C2SSelectSuggestionEffect msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MindStoneSuggestionEffect selected = MindStoneSuggestionEffect.fromOrdinal(msg.selected);
            // Store selection server-side, or pass to player NBT/state as you see fit
            // You may want to use PlayerCapability or similar for persistent selection
        });
        ctx.get().setPacketHandled(true);
    }
}
