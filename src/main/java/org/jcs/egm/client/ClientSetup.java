package org.jcs.egm.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jcs.egm.client.particle.PowerStoneEffectOne;
import org.jcs.egm.client.particle.RealityStoneEffectOne;
import org.jcs.egm.client.particle.SoulStoneEffectOne;
import org.jcs.egm.client.particle.TimeStoneEffectOne;
import org.jcs.egm.client.render.PowerStoneLightningRenderer;
import org.jcs.egm.registry.ModEntities;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.registry.ModMenuTypes;
import org.jcs.egm.registry.ModParticles;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(
                ModEntities.POWER_STONE_LIGHTNING.get(),
                PowerStoneLightningRenderer::new
        );
        event.enqueueWork(() -> {
            MenuScreens.register(
                    ModMenuTypes.INFINITY_GAUNTLET.get(),
                    InfinityGauntletScreen::new
            );
            MenuScreens.register(
                    ModMenuTypes.STONE_HOLDER.get(),
                    StoneHolderScreen::new
            );




            // Register custom item property for model overrides!
            ItemProperties.register(
                    ModItems.INFINITY_GAUNTLET.get(),
                    new ResourceLocation("stone_bitmask"),
                    (stack, world, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("StoneBitmask")) {
                            return stack.getTag().getInt("StoneBitmask");
                        }
                        return 0F;
                    }
            );
        });
    }


    // CUSTOM PARTICLE REGISTRATION //

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {

        // REALITY //
        event.registerSpriteSet(
                ModParticles.REALITY_STONE_EFFECT_ONE.get(),
                RealityStoneEffectOne.Provider::new);

        // SPACE //
        event.registerSpriteSet(
                ModParticles.POWER_STONE_EFFECT_ONE.get(),
                PowerStoneEffectOne.Provider::new);

        event.registerSpriteSet(
                ModParticles.POWER_STONE_EFFECT_TWO.get(),
                PowerStoneEffectOne.Provider::new);

        // TIME //
        event.registerSpriteSet(
                ModParticles.TIME_STONE_EFFECT_ONE.get(),
                TimeStoneEffectOne.Provider::new);

        // SOUL //
        event.registerSpriteSet(
                ModParticles.SOUL_STONE_EFFECT_ONE.get(),
                SoulStoneEffectOne.Provider::new);
    }
}
