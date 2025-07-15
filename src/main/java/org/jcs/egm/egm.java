package org.jcs.egm;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jcs.egm.config.ModCommonConfig;
import org.jcs.egm.dimension.ModDimensions;
import org.jcs.egm.registry.ModCreativeTab;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.registry.ModMenuTypes;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.network.NetworkHandler;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod(egm.MODID)
public class egm {
    public static final String MODID = "egm";

    public egm() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModCreativeTab.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.COMMON_CONFIG);
        modEventBus.addListener(this::setup); //
        ModDimensions.register(modEventBus);
    }

    private void setup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }
}
