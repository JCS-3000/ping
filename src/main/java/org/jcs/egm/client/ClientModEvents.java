package org.jcs.egm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.network.PossessionControlPacket;
import org.jcs.egm.stones.stone_mind.MindStoneOverlay;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@Mod.EventBusSubscriber(modid = "egm", bus = FORGE, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !MindStoneOverlay.isActive()) return;

        // Movement keys
        float strafe = 0f, forward = 0f;
        if (mc.options.keyUp.isDown())    forward += 1f;
        if (mc.options.keyDown.isDown())  forward -= 1f;
        if (mc.options.keyLeft.isDown())  strafe  += 1f;
        if (mc.options.keyRight.isDown()) strafe  -= 1f;
        boolean jump   = mc.options.keyJump.isDown();
        boolean attack = mc.options.keyAttack.isDown();

        // Mouse look
        float yaw   = player.getYRot();
        float pitch = player.getXRot();

        // Send to server
        NetworkHandler.INSTANCE.sendToServer(
                new PossessionControlPacket(strafe, forward, jump, attack, yaw, pitch)
        );

        // Suppress local player movement/inputs
        player.input.leftImpulse    = 0f;
        player.input.forwardImpulse = 0f;
        mc.options.keyJump.setDown(false);
        mc.options.keyAttack.setDown(false);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (MindStoneOverlay.isActive()) {
            event.setCanceled(true);
        }
    }
}
