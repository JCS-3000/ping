package org.jcs.egm.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class SoulRealmCommand {
    // Register with: SoulRealmCommand.register(dispatcher);
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("soulrealm")
                .requires(source -> source.hasPermission(2)) // OPs only, change as needed
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel soulRealm = player.server.getLevel(ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            new ResourceLocation("egm", "soul_realm")
                    ));

                    if (soulRealm == null) {
                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Soul Realm is not loaded!"));
                        return 0;
                    }

                    // Teleport player to same X/Z, safe Y
                    double x = player.getX();
                    double z = player.getZ();
                    double y = 80; // change if you want higher/lower

                    player.teleportTo(soulRealm, x, y, z, player.getYRot(), player.getXRot());
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Teleported to the Soul Realm"), false);
                    return 1;
                })
        );
    }
}
