package org.jcs.egm.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.network.PossessionControlPacket;
import org.jcs.egm.client.input.ClientPossessionTracker;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientPossessionInputHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != ClientTickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Use tracker, not overlay!
        if (!ClientPossessionTracker.isPossessing()) return;

        // read movement & look (direct keys, more reliable!)
        float strafe = 0f, forward = 0f;
        if (mc.options.keyUp.isDown())    forward += 1f;
        if (mc.options.keyDown.isDown())  forward -= 1f;
        if (mc.options.keyLeft.isDown())  strafe  += 1f;
        if (mc.options.keyRight.isDown()) strafe  -= 1f;
        boolean jump   = mc.options.keyJump.isDown();
        boolean attack = mc.options.keyAttack.isDown();
        float yaw      = player.getYRot();
        float pitch    = player.getXRot();

        // send to server
        NetworkHandler.INSTANCE.sendToServer(
                new PossessionControlPacket(strafe, forward, jump, attack, yaw, pitch)
        );

        // suppress default player movement
        player.input.leftImpulse    = 0F;
        player.input.forwardImpulse = 0F;
    }
}
