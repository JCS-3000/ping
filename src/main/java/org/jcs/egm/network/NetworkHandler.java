package org.jcs.egm.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
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

        INSTANCE.registerMessage(
                id++,
                GauntletSelectedStonePacket.class,
                GauntletSelectedStonePacket::encode,
                GauntletSelectedStonePacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        // Start possession → client
        INSTANCE.registerMessage(
                id++,
                StartPossessionPacket.class,
                StartPossessionPacket::encode,
                StartPossessionPacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // End possession → client
        INSTANCE.registerMessage(
                id++,
                EndPossessionPacket.class,
                EndPossessionPacket::encode,
                EndPossessionPacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Possession control → server
        INSTANCE.registerMessage(
                id++,
                PossessionControlPacket.class,
                PossessionControlPacket::encode,
                PossessionControlPacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    /** Server → client: start overlay & camera switch */
    public static void sendStartPossessionPacket(ServerPlayer player, int duration, int entityId) {
        INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StartPossessionPacket(duration, entityId)
        );
    }

    /** Server → client: end overlay & reset camera */
    public static void sendEndPossessionPacket(ServerPlayer player) {
        INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new EndPossessionPacket()
        );
    }
}