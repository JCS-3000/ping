package org.jcs.egm.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jcs.egm.client.input.GauntletSelectedStonePacket;
import org.jcs.egm.egm;

import java.util.Optional;

public class NetworkHandler {
    public static final String PROTOCOL = "1.0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(egm.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    public static void register() {
        int id = 0;

        // Gauntlet: client → server (select stone)
        INSTANCE.registerMessage(
                id++,
                GauntletSelectedStonePacket.class,
                GauntletSelectedStonePacket::encode,
                GauntletSelectedStonePacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        // Gauntlet menu open (client → server)
        INSTANCE.registerMessage(
                id++,
                OpenGauntletMenuPacket.class,
                OpenGauntletMenuPacket::encode,
                OpenGauntletMenuPacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        INSTANCE.registerMessage(
                id++,
                C2SSetStoneAbilityIndex.class,
                C2SSetStoneAbilityIndex::toBytes,
                C2SSetStoneAbilityIndex::new,
                C2SSetStoneAbilityIndex::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        INSTANCE.registerMessage(
                id++,
                OpenStoneHolderMenuPacket.class,
                OpenStoneHolderMenuPacket::encode,
                OpenStoneHolderMenuPacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        INSTANCE.registerMessage(
                id++,
                SetAbilityIndexPacket.class,
                SetAbilityIndexPacket::encode,
                SetAbilityIndexPacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}
