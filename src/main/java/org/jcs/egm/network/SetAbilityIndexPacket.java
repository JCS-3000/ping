package org.jcs.egm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.StoneHolderItem;

import java.util.function.Supplier;

public class SetAbilityIndexPacket {
    private final int index;
    private final InteractionHand hand;

    public SetAbilityIndexPacket(int index, InteractionHand hand) {
        this.index = index;
        this.hand = hand;
    }

    public SetAbilityIndexPacket(FriendlyByteBuf buf) {
        this.index = buf.readVarInt();
        this.hand = buf.readEnum(InteractionHand.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(index);
        buf.writeEnum(hand);
    }

    public static SetAbilityIndexPacket decode(FriendlyByteBuf buf) {
        return new SetAbilityIndexPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty()) {
                // For Stone Holder
                if (stack.getItem() instanceof StoneHolderItem) {
                    ItemStack inside = StoneHolderItem.getStone(stack);
                    if (!inside.isEmpty()) {
                        inside.getOrCreateTag().putInt("AbilityIndex", index);
                        StoneHolderItem.setStone(stack, inside);
                    }
                }
                // For Gauntlet
                else if (stack.getItem() instanceof InfinityGauntletItem) {
                    int stoneIdx = InfinityGauntletItem.getSelectedStone(stack);
                    ItemStackHandler handler = new ItemStackHandler(6);
                    if (stack.hasTag() && stack.getTag().contains("Stones")) {
                        handler.deserializeNBT(stack.getTag().getCompound("Stones"));
                    }
                    ItemStack stoneStack = handler.getStackInSlot(stoneIdx);
                    if (!stoneStack.isEmpty()) {
                        stoneStack.getOrCreateTag().putInt("AbilityIndex", index);
                        handler.setStackInSlot(stoneIdx, stoneStack);
                        // Save handler back to gauntlet
                        stack.getTag().put("Stones", handler.serializeNBT());
                    }
                }
                // For raw stone or fallback
                else {
                    stack.getOrCreateTag().putInt("AbilityIndex", index);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
