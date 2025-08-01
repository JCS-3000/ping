package org.jcs.egm.registry;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.command.SoulRealmCommand;
import org.jcs.egm.egm;
import org.jcs.egm.stones.stone_soul.SoulStonePassiveHandler;

@Mod.EventBusSubscriber(modid = egm.MODID)
public class ModEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SoulRealmCommand.register(event.getDispatcher());
        SoulStonePassiveHandler.register(event.getDispatcher());
    }
}
