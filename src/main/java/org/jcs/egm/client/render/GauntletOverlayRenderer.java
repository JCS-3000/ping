package org.jcs.egm.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.egm;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.stones.StoneItem;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = egm.MODID, value = Dist.CLIENT)
public class GauntletOverlayRenderer {

    private static long lastScrollTime = 0;
    private static int lastSelectedSlot = -1;
    private static List<StoneEntry> cachedPresentStones = new ArrayList<>();

    // --- You must call this when the scroll wheel is used ---
    public static void updateSelectedStone(int selectedSlot, ItemStack gauntlet) {
        lastScrollTime = System.currentTimeMillis();
        lastSelectedSlot = selectedSlot;
        cachedPresentStones = buildPresentStones(gauntlet);
    }

    // Builds a list of non-empty stone slots
    private static List<StoneEntry> buildPresentStones(ItemStack gauntlet) {
        List<StoneEntry> stones = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ItemStack s = InfinityGauntletItem.getStoneStack(gauntlet, i);
            if (!s.isEmpty() && s.getItem() instanceof StoneItem) {
                stones.add(new StoneEntry(i, s));
            }
        }
        return stones;
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        long elapsed = System.currentTimeMillis() - lastScrollTime;
        if (elapsed > 1500) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof InfinityGauntletItem)) return;

        // --- Only show overlay if user is holding the gauntlet and recently scrolled ---
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        //
        // HEIGHT ABOVE XP BAR
        //
        int y = screenHeight - 34;

        String displayText;
        int color;

        // Use cached present stones (updated by updateSelectedStone) for consistency with scroll
        if (cachedPresentStones.isEmpty()) {
            displayText = "No stone present";
            color = 0xAAAAAA; // gray
        } else {
            // Try to find the matching slot in the cached list; fall back to the first
            StoneEntry match = cachedPresentStones.stream()
                    .filter(e -> e.slot == lastSelectedSlot)
                    .findFirst()
                    .orElse(cachedPresentStones.get(0));

            // Use the display name and color of the stone item
            displayText = match.stack.getHoverName().getString();
            color = ((StoneItem) match.stack.getItem()).getColor();
        }

        // Draw centered string, color with opaque alpha
        GuiGraphics graphics = event.getGuiGraphics();
        int textWidth = mc.font.width(displayText);
        graphics.drawString(
                mc.font,
                displayText,
                screenWidth / 2 - textWidth / 2,
                y,
                color | 0xFF000000, // Force alpha
                true
        );
    }

    // Simple class for pairing slot index and stone stack
    private static class StoneEntry {
        final int slot;
        final ItemStack stack;
        StoneEntry(int slot, ItemStack stack) { this.slot = slot; this.stack = stack; }
    }
}
