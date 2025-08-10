package org.jcs.egm.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE, modid = "egm")
public final class ShockwaveShakeHandler {
    private static int remaining;
    private static float baseIntensity;
    private static final Random RNG = new Random();

    private static float phaseYaw, phasePitch, phaseBob;

    private ShockwaveShakeHandler() {}

    public static void trigger(int durationTicks, float intensity0to1) {
        if (durationTicks <= 0 || intensity0to1 <= 0f) return;
        remaining = Math.max(remaining, Math.min(200, durationTicks));
        baseIntensity = Math.max(baseIntensity, Mth.clamp(intensity0to1, 0f, 2f));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (remaining > 0) {
            remaining--;
            phaseYaw = RNG.nextFloat() * (float)Math.PI * 2f;
            phasePitch = RNG.nextFloat() * (float)Math.PI * 2f;
            phaseBob = RNG.nextFloat() * (float)Math.PI * 2f;
            if (remaining == 0) baseIntensity = 0f;
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles e) {
        if (remaining <= 0 || baseIntensity <= 0f) return;
        float falloff = Mth.clamp(remaining / 10f, 0f, 1f);
        float yawAmpDeg = 1.2f * baseIntensity * falloff;
        float pitchAmpDeg = 0.8f * baseIntensity * falloff;
        e.setYaw(e.getYaw() + (float)Math.sin(phaseYaw) * yawAmpDeg);
        e.setPitch(e.getPitch() + (float)Math.cos(phasePitch) * pitchAmpDeg);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent e) {
        if (remaining <= 0 || baseIntensity <= 0f) return;
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (Minecraft.getInstance().player == null) return;

        PoseStack pose = e.getPoseStack();
        if (pose == null) return;

        float falloff = Mth.clamp(remaining / 10f, 0f, 1f);
        float bob = 0.02f * baseIntensity * falloff;
        float dx = (float)Math.sin(phaseBob) * bob;
        float dy = (float)Math.cos(phaseBob) * bob * 0.6f;

        // Stage-local PoseStack is ephemeral; no need to push/pop.
        pose.translate(dx, dy, 0.0f);
    }
}
