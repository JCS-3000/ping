package org.jcs.egm.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import org.jcs.egm.stones.StoneAbilityCooldowns;

public class ToggleCooldownsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("toggle_stone_cooldowns")
                        .requires(source -> source.hasPermission(2)) // OP-only
                        .executes(ctx -> {
                            StoneAbilityCooldowns.toggleCooldowns();
                            boolean disabled = StoneAbilityCooldowns.areCooldownsDisabled();
                            
                            Component message = Component.literal("Stone cooldowns are now ")
                                    .append(Component.literal(disabled ? "DISABLED" : "ENABLED")
                                            .withStyle(Style.EMPTY
                                                    .withColor(disabled ? ChatFormatting.RED : ChatFormatting.GREEN)
                                                    .withBold(true)))
                                    .withStyle(Style.EMPTY.withItalic(true));
                            
                            ctx.getSource().sendSystemMessage(message);
                            return 1;
                        })
        );
    }
}