package org.jcs.egm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import org.jcs.egm.gauntlet.InfinityGauntletMenu;

import java.util.function.Supplier;

public class OpenGauntletMenuPacket {

    // No data needed
    public OpenGauntletMenuPacket() {}

    public OpenGauntletMenuPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static OpenGauntletMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenGauntletMenuPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem) {
                NetworkHooks.openScreen(
                        player,
                        new InfinityGauntletMenu.Provider(mainHand),
                        buf -> buf.writeItem(mainHand)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
