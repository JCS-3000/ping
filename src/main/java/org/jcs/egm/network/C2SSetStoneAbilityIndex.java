package org.jcs.egm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.holders.StoneHolderItem;
import org.jcs.egm.gauntlet.InfinityGauntletItem;

import java.util.function.Supplier;

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

            // Stone Holder
            if (stack.getItem() instanceof StoneHolderItem) {
                var inside = StoneHolderItem.getStone(stack);
                if (!inside.isEmpty()) {
                    inside.getOrCreateTag().putInt("AbilityIndex", index);
                    StoneHolderItem.setStone(stack, inside);
                }
            }
            // Gauntlet
            else if (stack.getItem() instanceof InfinityGauntletItem) {
                int stoneIdx = InfinityGauntletItem.getSelectedStone(stack);
                ItemStackHandler handler = new ItemStackHandler(6);
                if (stack.hasTag() && stack.getTag().contains("Stones")) {
                    handler.deserializeNBT(stack.getTag().getCompound("Stones"));
                }
                var stoneStack = handler.getStackInSlot(stoneIdx);
                if (!stoneStack.isEmpty()) {
                    stoneStack.getOrCreateTag().putInt("AbilityIndex", index);
                    handler.setStackInSlot(stoneIdx, stoneStack);
                    stack.getTag().put("Stones", handler.serializeNBT());
                }
            }
            // Raw stone or fallback
            else {
                stack.getOrCreateTag().putInt("AbilityIndex", index);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
