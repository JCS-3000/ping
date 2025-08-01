package org.jcs.egm.registry;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.command.SoulRealmCommand;
import org.jcs.egm.command.SoulStoneRezResetCommand;
import org.jcs.egm.egm;

@Mod.EventBusSubscriber(modid = egm.MODID)
public class ModEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SoulRealmCommand.register(event.getDispatcher());
        SoulStoneRezResetCommand.register(event.getDispatcher());
    }
}
