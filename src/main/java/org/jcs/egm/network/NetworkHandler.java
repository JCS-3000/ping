package org.jcs.egm.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jcs.egm.client.input.GauntletSelectedStonePacket;
import org.jcs.egm.egm;

public class NetworkHandler {
    public static final String PROTOCOL = "1.0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(egm.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(
                id++,
                GauntletSelectedStonePacket.class,
                GauntletSelectedStonePacket::encode,
                GauntletSelectedStonePacket::decode,
                (msg, ctx) -> { msg.handle(ctx); });
        INSTANCE.registerMessage(

                id++,
                PowerBeamPacket.class,
                PowerBeamPacket::encode,
                PowerBeamPacket::decode,
                PowerBeamPacket::handle
        );
    }
}
