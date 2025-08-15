package org.jcs.egm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.client.input.InfinityKeybinds;
import org.jcs.egm.client.particle.UniversalTintParticle;
import org.jcs.egm.egm;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.StoneHolderItem;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.network.OpenGauntletMenuPacket;
import org.jcs.egm.network.OpenStoneHolderMenuPacket;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.StoneItem;
import org.jcs.egm.registry.ModEffects;
import org.jcs.egm.stones.StoneAbilityRegistries;
import org.jcs.egm.stones.stone_power.EmpoweredPunchPowerStoneAbility;
import org.jcs.egm.stones.stone_power.PowerStoneItem;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientModEvents {

    private static int particleTick = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack mainHand = mc.player.getMainHandItem();
                
                // Check for empowered punch particles (every 2 ticks)
                particleTick++;
                if (particleTick % 2 == 0) {
                    if (mc.player.hasEffect(ModEffects.EMPOWERED_PUNCH.get())) {
                        // Show empowered punch effect particles around the player
                        var empoweredPunch = new EmpoweredPunchPowerStoneAbility();
                        empoweredPunch.spawnChargedParticlesPublic(mc.level, mc.player);
                    }
                }
                
                if (InfinityKeybinds.OPEN_STONE_MENU.consumeClick()) {
                    if (!mainHand.isEmpty()) {
                        if (mainHand.getItem() == ModItems.INFINITY_GAUNTLET.get()) {
                            NetworkHandler.INSTANCE.sendToServer(new OpenGauntletMenuPacket());
                        } else if (mainHand.getItem() instanceof StoneHolderItem) {
                            NetworkHandler.INSTANCE.sendToServer(new OpenStoneHolderMenuPacket());
                        }
                    }
                }
            }
        }
        if (InfinityKeybinds.OPEN_ABILITY_MENU.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack stack = mc.player.getMainHandItem();
                if (stack.getItem() instanceof StoneItem stoneItem) {
                    StoneItem.openStoneAbilityMenu(stack, InteractionHand.MAIN_HAND, stoneItem.getKey());
                } else if (stack.getItem() instanceof StoneHolderItem holder) {
                    StoneItem.openStoneAbilityMenu(stack, InteractionHand.MAIN_HAND, holder.getStoneKey());
                } else if (stack.getItem() instanceof InfinityGauntletItem) {
                    String stoneKey = InfinityGauntletItem.getSelectedStoneName(stack);
                    StoneItem.openStoneAbilityMenu(stack, InteractionHand.MAIN_HAND, stoneKey);
                }
            }
        }
    }
}

