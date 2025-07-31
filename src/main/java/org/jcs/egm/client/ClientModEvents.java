package org.jcs.egm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.client.input.InfinityKeybinds;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.network.OpenGauntletMenuPacket;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.StoneItem;
import org.jcs.egm.stones.stone_mind.MindStoneAbility;
import org.jcs.egm.stones.stone_mind.MindStoneSuggestionEffect;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Gauntlet menu open with H
            if (InfinityKeybinds.OPEN_GAUNTLET_MENU.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    ItemStack mainHand = mc.player.getMainHandItem();
                    if (!mainHand.isEmpty() && mainHand.getItem() == ModItems.INFINITY_GAUNTLET.get()) {
                        NetworkHandler.INSTANCE.sendToServer(new OpenGauntletMenuPacket());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(InputEvent.MouseButton.Pre event) {
        if (event.getButton() == 0 && event.getAction() == 1) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack mainHand = mc.player.getMainHandItem();
                boolean allow = false;
                if (!mainHand.isEmpty() && mainHand.getItem() instanceof StoneItem stone && "mind".equals(stone.getKey())) {
                    allow = true;
                } else if (!mainHand.isEmpty() && mainHand.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem) {
                    String stoneName = org.jcs.egm.gauntlet.InfinityGauntletItem.getSelectedStoneName(mainHand);
                    if ("mind".equals(stoneName)) allow = true;
                }
                if (allow) {
                    mc.setScreen(new org.jcs.egm.client.MindStoneScreen());
                    event.setCanceled(true); // Prevent normal attack
                }
            }
        }
    }

    // --- Mind Stone Suggestion effect handling ---
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player == null || !player.level().isClientSide) return;

        MindStoneSuggestionEffect effect = MindStoneAbility.pendingEffect;
        if (effect == null) return;

        ItemStack main = player.getMainHandItem();
        boolean allow = false;
        if (main.getItem() instanceof StoneItem stone && "mind".equals(stone.getKey())) {
            allow = true;
        } else if (main.getItem() instanceof org.jcs.egm.gauntlet.InfinityGauntletItem) {
            String stoneName = org.jcs.egm.gauntlet.InfinityGauntletItem.getSelectedStoneName(main);
            if ("mind".equals(stoneName)) {
                allow = true;
            }
        }
        if (!allow) return;

        if (event.getTarget() instanceof LivingEntity le) {
            MindStoneAbility.tryApplyEffectToEntity(player, le, effect);
            MindStoneAbility.pendingEffect = null;
            event.setCanceled(true);
        }
    }
}
