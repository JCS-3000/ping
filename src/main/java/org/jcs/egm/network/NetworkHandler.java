package org.jcs.egm.network;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jcs.egm.client.input.GauntletSelectedStonePacket;
import org.jcs.egm.egm;

// If these classes are in a subpackage (e.g. org.jcs.egm.network.packet), update the imports.
import org.jcs.egm.network.RingFXPacket;
import org.jcs.egm.network.TintedParticlePacket;

import java.util.Optional;

public class NetworkHandler {

    public static final String PROTOCOL = "1.0";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(egm.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    /** Call once during common setup. */
    public static void register() {
        int id = 0;

        // === C2S ===
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

        // Stone ability index (client → server)
        INSTANCE.registerMessage(
                id++,
                C2SSetStoneAbilityIndex.class,
                C2SSetStoneAbilityIndex::toBytes,
                C2SSetStoneAbilityIndex::new,
                C2SSetStoneAbilityIndex::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        // Stone Holder menu open (client → server)
        INSTANCE.registerMessage(
                id++,
                OpenStoneHolderMenuPacket.class,
                OpenStoneHolderMenuPacket::encode,
                OpenStoneHolderMenuPacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        // Ability index (client → server)
        INSTANCE.registerMessage(
                id++,
                SetAbilityIndexPacket.class,
                SetAbilityIndexPacket::encode,
                SetAbilityIndexPacket::decode,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        // === S2C ===
        // Camera shake (server → client)
        INSTANCE.registerMessage(
                id++,
                org.jcs.egm.network.packet.ShakeCameraPacket.class,
                org.jcs.egm.network.packet.ShakeCameraPacket::encode,
                org.jcs.egm.network.packet.ShakeCameraPacket::decode,
                org.jcs.egm.network.packet.ShakeCameraPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Morph sync (server → client)
        INSTANCE.registerMessage(
                id++,
                org.jcs.egm.stones.stone_reality.S2CSyncMorph.class,
                org.jcs.egm.stones.stone_reality.S2CSyncMorph::encode,
                org.jcs.egm.stones.stone_reality.S2CSyncMorph::decode,
                org.jcs.egm.stones.stone_reality.S2CSyncMorph::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Wrist ring (server → client)
        INSTANCE.registerMessage(
                id++,
                RingFXPacket.class,
                RingFXPacket::encode,
                RingFXPacket::new,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Tinted particle (server → client)
        INSTANCE.registerMessage(
                id++,
                TintedParticlePacket.class,
                TintedParticlePacket::encode,
                TintedParticlePacket::new,
                (msg, ctx) -> msg.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    // =========================
    // Convenience send helpers
    // =========================

    /** Broadcast the charging wrist ring to everyone tracking this player (and the player themself). */
    public static void sendWristRing(Player player, int ticksHeld, int colorAHex, int colorBHex) {
        if (!(player.level() instanceof ServerLevel)) return;
        INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new RingFXPacket(player.getId(), ticksHeld, colorAHex, colorBHex)
        );
    }

    /** Send a tinted particle spawn to all players in the level's dimension. */
    public static void sendTintedParticle(ServerLevel sl,
                                          ParticleType<?> type,
                                          double x, double y, double z,
                                          float r, float g, float b) {
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        if (id == null) return; // unknown type
        INSTANCE.send(
                PacketDistributor.DIMENSION.with(sl::dimension),
                new TintedParticlePacket(id, x, y, z, r, g, b)
        );
    }
}
