package org.jcs.egm.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jcs.egm.stones.stone_mind.*;

public class MindStonePackets {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("egm", "mindstone"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int i = 0;
        CHANNEL.registerMessage(i++, C2SSelectSuggestionEffect.class, C2SSelectSuggestionEffect::encode, C2SSelectSuggestionEffect::decode, C2SSelectSuggestionEffect::handle);
        CHANNEL.registerMessage(i++, C2SApplySuggestionEffect.class, C2SApplySuggestionEffect::encode, C2SApplySuggestionEffect::decode, C2SApplySuggestionEffect::handle);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
