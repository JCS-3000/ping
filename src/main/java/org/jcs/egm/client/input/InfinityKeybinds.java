package org.jcs.egm.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public class InfinityKeybinds {
    public static final KeyMapping OPEN_GAUNTLET_MENU = new KeyMapping(
            "key.egm.open_gauntlet_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.inventory"
    );
    public static final KeyMapping OPEN_MIND_WHEEL = new KeyMapping(
            "key.egm.open_mind_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.inventory"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GAUNTLET_MENU);
        event.register(OPEN_MIND_WHEEL);
    }
}
