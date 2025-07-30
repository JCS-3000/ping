package org.jcs.egm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.client.input.ClientPossessionTracker;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.network.PossessionControlPacket;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@Mod.EventBusSubscriber(modid = "egm", bus = FORGE, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (ClientPossessionTracker.isPossessing()) {
            event.setCanceled(true);
        }
    }
}
