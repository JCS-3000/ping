package org.jcs.egm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.holders.StoneHolderItem;

import java.util.function.Supplier;

/**
 * Packet: Sent from client to server when a player selects an ability for a stone.
 * Carries: hand (main/off), selected index.
 */
public class C2SSetStoneAbilityIndex {
    private final int hand; // 0 = main, 1 = off
    private final int index; // selected ability index

    public C2SSetStoneAbilityIndex(int hand, int index) {
        this.hand = hand;
        this.index = index;
    }

    public C2SSetStoneAbilityIndex(FriendlyByteBuf buf) {
        this.hand = buf.readVarInt();
        this.index = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(hand);
        buf.writeVarInt(index);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var stack = player.getItemInHand(hand == 0 ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND);
            if (stack.getItem() instanceof org.jcs.egm.stones.StoneItem) {
                stack.getOrCreateTag().putInt("AbilityIndex", index);
            } else if (stack.getItem() instanceof StoneHolderItem) {
                stack.getOrCreateTag().putInt("AbilityIndex", index);
            } else if (stack.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem) {
                stack.getOrCreateTag().putInt("AbilityIndex", index);
            }
            // Add additional logic here if you need per-context ability storage (e.g., for gauntlet per-stone)
        });
        ctx.get().setPacketHandled(true);
    }
}
