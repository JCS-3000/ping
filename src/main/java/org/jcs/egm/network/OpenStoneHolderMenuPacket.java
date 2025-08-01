package org.jcs.egm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jcs.egm.holders.StoneHolderItem;
import org.jcs.egm.holders.StoneHolderMenu;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenStoneHolderMenuPacket {

    public OpenStoneHolderMenuPacket() {}
    public OpenStoneHolderMenuPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public static OpenStoneHolderMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenStoneHolderMenuPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack mainHand = player.getMainHandItem();
            // Replace StoneHolderItem with your actual holder class
            if (!mainHand.isEmpty() && mainHand.getItem() instanceof StoneHolderItem) {
                NetworkHooks.openScreen(
                        player,
                        new StoneHolderMenu.Provider(mainHand),
                        buf -> buf.writeItem(mainHand)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
