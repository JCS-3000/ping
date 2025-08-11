package org.jcs.egm.stones.stone_reality;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import org.jcs.egm.network.NetworkHandler; // your existing handler

import java.util.UUID;
import java.util.function.Supplier;

public class S2CSyncMorph {

    public final UUID playerId;
    public final ResourceLocation mobId; // nullable
    public final long expiresAt;
    public final byte flags;

    public S2CSyncMorph(UUID playerId, ResourceLocation mobId, long expiresAt, byte flags) {
        this.playerId = playerId;
        this.mobId = mobId;
        this.expiresAt = expiresAt;
        this.flags = flags;
    }

    public static void register(int id) {
        NetworkHandler.INSTANCE.registerMessage(
                id,
                S2CSyncMorph.class,
                S2CSyncMorph::encode,
                S2CSyncMorph::decode,
                S2CSyncMorph::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }


    public static void send(Player target, MorphData.MorphState state) {
        if (target.level().isClientSide) return;
        NetworkHandler.INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new S2CSyncMorph(target.getUUID(), state.mobId, state.expiresAt, state.flags)
        );
    }

    public static void encode(S2CSyncMorph msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.mobId != null);
        if (msg.mobId != null) buf.writeResourceLocation(msg.mobId);
        buf.writeLong(msg.expiresAt);
        buf.writeByte(msg.flags);
    }

    public static S2CSyncMorph decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        ResourceLocation mob = buf.readBoolean() ? buf.readResourceLocation() : null;
        long ex = buf.readLong();
        byte fl = buf.readByte();
        return new S2CSyncMorph(id, mob, ex, fl);
    }

    public static void handle(S2CSyncMorph msg, Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null) return;
            var p = level.getPlayerByUUID(msg.playerId);
            if (p == null) return;

            p.getCapability(MorphData.CAP).ifPresent(state -> {
                state.mobId = msg.mobId;
                state.expiresAt = msg.expiresAt;
                state.flags = msg.flags;
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
