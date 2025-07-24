package org.jcs.egm.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class SoulRealmCommand {
    // Register with: SoulRealmCommand.register(dispatcher);
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("soulrealm")
                .requires(source -> source.hasPermission(2)) // OPs only
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    // Use ResourceKey<Level>, not <ServerLevel>!
                    ResourceKey<Level> soulRealmKey = ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse("egm:soul_realm")
                    );
                    ResourceKey<Level> overworldKey = Level.OVERWORLD;

                    ServerLevel soulRealm = player.server.getLevel(soulRealmKey);
                    ServerLevel overworld = player.server.getLevel(overworldKey);

                    if (soulRealm == null || overworld == null) {
                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Dimension is not loaded!"));
                        return 0;
                    }

                    double x = player.getX();
                    double z = player.getZ();
                    double y = 56; // use your safe Y

                    if (player.level().dimension().equals(soulRealmKey)) {
                        player.teleportTo(overworld, x, y, z, player.getYRot(), player.getXRot());
                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Returned to the Overworld"), false);
                    } else {
                        player.teleportTo(soulRealm, x, y, z, player.getYRot(), player.getXRot());
                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Teleported to the Soul Realm"), false);
                    }
                    return 1;
                })
        );
    }
}
