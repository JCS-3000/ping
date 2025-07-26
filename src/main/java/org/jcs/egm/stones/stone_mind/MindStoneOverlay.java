package org.jcs.egm.stones.stone_mind;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class MindStoneOverlay {
    private static final ResourceLocation BLUR = new ResourceLocation("egm", "textures/misc/mind_stone_blur.png");

    private static boolean active = false;
    private static int ticksRemaining = 0;

    public static boolean isActive() {
        return active;
    }
    // Starts the overlay with a given duration in ticks (flicker effect uses this)
    public static void start(int durationTicks) {
        active = true;
        ticksRemaining = durationTicks;
    }

    // Stops overlay
    public static void stop() {
        active = false;
        ticksRemaining = 0;
    }

    // Render overlay if active
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!active) return;

        // Flicker effect for last 10 seconds (200 ticks)
        if (ticksRemaining > 0 && ticksRemaining <= 200) {
            // Flicker every 10 ticks: only render if even 10s
            if ((ticksRemaining / 10) % 2 == 0) return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        RenderSystem.enableBlend();

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        guiGraphics.blit(BLUR, 0, 0, 0, 0, width, height, 512, 256);

        RenderSystem.disableBlend();
    }
}
