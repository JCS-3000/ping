package org.jcs.egm.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.network.PossessionControlPacket;
import org.jcs.egm.stones.stone_mind.MindStoneOverlay;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientPossessionInputHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != ClientTickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // only while possessed (overlay active)
        if (!MindStoneOverlay.isActive()) return;

        // read movement & look
        Input input = player.input;
        float strafe  = input.leftImpulse;
        float forward = input.forwardImpulse;
        boolean jump  = mc.options.keyJump.isDown();
        boolean attack= mc.options.keyAttack.isDown();
        float yaw     = player.getYRot();
        float pitch   = player.getXRot();

        // send to server
        NetworkHandler.INSTANCE.sendToServer(
                new PossessionControlPacket(strafe, forward, jump, attack, yaw, pitch)
        );

        // suppress default player movement
        input.leftImpulse  = 0F;
        input.forwardImpulse = 0F;
    }
}
