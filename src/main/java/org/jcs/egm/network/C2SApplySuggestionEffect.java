package org.jcs.egm.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import org.jcs.egm.stones.stone_mind.MindStoneAbility;
import org.jcs.egm.stones.stone_mind.MindStoneSuggestionEffect;

import java.util.function.Supplier;

public class C2SApplySuggestionEffect {
    public final int targetId;
    public final int effectOrdinal;

    public C2SApplySuggestionEffect(int targetId, MindStoneSuggestionEffect effect) {
        this.targetId = targetId;
        this.effectOrdinal = effect.ordinal();
    }

    public static void encode(C2SApplySuggestionEffect msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeVarInt(msg.targetId);
        buf.writeVarInt(msg.effectOrdinal);
    }

    public static C2SApplySuggestionEffect decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new C2SApplySuggestionEffect(buf.readVarInt(), MindStoneSuggestionEffect.fromOrdinal(buf.readVarInt()));
    }

    public static void handle(C2SApplySuggestionEffect msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            ServerLevel level = sender.serverLevel();
            Entity target = level.getEntity(msg.targetId);
            if (target instanceof LivingEntity le) {
                MindStoneAbility.applyEffectServer(sender, le, MindStoneSuggestionEffect.fromOrdinal(msg.effectOrdinal));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
