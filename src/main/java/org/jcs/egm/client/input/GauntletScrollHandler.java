package org.jcs.egm.client.input;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.jcs.egm.client.render.GauntletOverlayRenderer;
import org.jcs.egm.egm;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.stones.StoneItem;
import org.jcs.egm.network.NetworkHandler;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = egm.MODID, value = Dist.CLIENT)
public class GauntletScrollHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof InfinityGauntletItem)) return;

        // 1. Build a list of slot indices for stones that are present in the gauntlet
        List<Integer> presentIndices = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ItemStack s = InfinityGauntletItem.getStoneStack(stack, i);
            if (!s.isEmpty() && s.getItem() instanceof StoneItem) {
                presentIndices.add(i);
            }
        }

        // 2. If no stones, update overlay and do nothing else
        if (presentIndices.isEmpty()) {
            GauntletOverlayRenderer.updateSelectedStone(-1, stack); // -1 = none
            return;
        }

        // 3. Find index of currently selected stone among the present ones
        int currentSlot = InfinityGauntletItem.getSelectedStone(stack);
        int currentPresentIdx = presentIndices.indexOf(currentSlot);
        if (currentPresentIdx == -1) currentPresentIdx = 0; // Default to first present

        // 4. Determine scroll direction
        double scrollDelta = event.getScrollDelta();
        int direction = scrollDelta > 0 ? 1 : -1;

        // 5. Cycle through present stones only
        int nextIdx = (currentPresentIdx + direction + presentIndices.size()) % presentIndices.size();
        int nextSlot = presentIndices.get(nextIdx);

        // 6. Update selected stone NBT
        InfinityGauntletItem.setSelectedStone(stack, nextSlot);

        // 7. Update overlay with new selection and send packet to server
        GauntletOverlayRenderer.updateSelectedStone(nextSlot, stack);
        NetworkHandler.INSTANCE.sendToServer(new GauntletSelectedStonePacket(nextSlot));

        event.setCanceled(true);
    }
}
