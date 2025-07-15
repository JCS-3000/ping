package org.jcs.egm.client.input;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.gauntlet.InfinityGauntletItem;

import java.util.function.Supplier;

public class GauntletSelectedStonePacket {
    private final int selected;

    public GauntletSelectedStonePacket(int selected) {
        this.selected = selected;
    }

    public static void encode(GauntletSelectedStonePacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.selected);
    }

    public static GauntletSelectedStonePacket decode(FriendlyByteBuf buf) {
        return new GauntletSelectedStonePacket(buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof InfinityGauntletItem) {
                    InfinityGauntletItem.setSelectedStone(stack, selected);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
