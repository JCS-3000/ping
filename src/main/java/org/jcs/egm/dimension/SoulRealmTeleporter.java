package org.jcs.egm.dimension;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class SoulRealmTeleporter {
    public static void teleport(ServerPlayer player) {
        ServerLevel soulRealm = player.server.getLevel(ModDimensions.SOUL_REALM);
        if (soulRealm != null) {
            // Teleport to Y=75; adjust X/Z as desired
            player.teleportTo(soulRealm, player.getX(), 75, player.getZ(), player.getYRot(), player.getXRot());
        }
    }
}