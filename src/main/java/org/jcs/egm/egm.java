package org.jcs.egm;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jcs.egm.config.ModCommonConfig;
import org.jcs.egm.dimension.ModDimensions;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.registry.*;

@Mod(egm.MODID)
public class egm {
    public static final String MODID = "egm";

    public egm() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModDimensions.register(modEventBus);
        modEventBus.addListener(this::setup);
        ModCreativeTab.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.COMMON_CONFIG);
    }

    private void setup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }
}
