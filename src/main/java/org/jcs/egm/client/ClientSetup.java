package org.jcs.egm.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jcs.egm.client.particle.*;
import org.jcs.egm.egm;
import org.jcs.egm.client.render.MeteorRenderer;
import org.jcs.egm.client.render.PowerStoneLightningRenderer;
import org.jcs.egm.client.render.SingularityRenderer;
import org.jcs.egm.registry.ModEntities;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.registry.ModMenuTypes;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.client.render.TimeBubbleFieldRenderer;

@Mod.EventBusSubscriber(modid = egm.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(
                ModEntities.POWER_STONE_LIGHTNING.get(),
                PowerStoneLightningRenderer::new
        );
        EntityRenderers.register(
                ModEntities.TIME_ACCEL_FIELD.get(),
                TimeBubbleFieldRenderer::new
        );
        EntityRenderers.register(
                ModEntities.SINGULARITY.get(),
                SingularityRenderer::new
        );
        EntityRenderers.register(
                ModEntities.METEOR.get(),
                MeteorRenderer::new
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

            // Infinity Gauntlet and Holder: stone_bitmask predicate (uses StoneBitmask NBT)
            ItemProperties.register(
                    ModItems.INFINITY_GAUNTLET.get(),
                    new ResourceLocation(egm.MODID, "stone_bitmask"),
                    (stack, world, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("StoneBitmask")) {
                            return stack.getTag().getInt("StoneBitmask");
                        }
                        return 0F;
                    }
            );
            ItemProperties.register(
                    ModItems.MIND_STONE_HOLDER.get(),
                    new ResourceLocation(egm.MODID, "stone_bitmask"),
                    (stack, world, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("StoneBitmask")) {
                            return stack.getTag().getInt("StoneBitmask");
                        }
                        return 0F;
                    }
            );
            ItemProperties.register(
                    ModItems.POWER_STONE_HOLDER.get(),
                    new ResourceLocation(egm.MODID, "stone_bitmask"),
                    (stack, world, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("StoneBitmask")) {
                            return stack.getTag().getInt("StoneBitmask");
                        }
                        return 0F;
                    }
            );
            ItemProperties.register(
                    ModItems.SPACE_STONE_HOLDER.get(),
                    new ResourceLocation(egm.MODID, "stone_bitmask"),
                    (stack, world, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("StoneBitmask")) {
                            return stack.getTag().getInt("StoneBitmask");
                        }
                        return 0F;
                    }
            );
            ItemProperties.register(
                    ModItems.REALITY_STONE_HOLDER.get(),
                    new ResourceLocation(egm.MODID, "stone_bitmask"),
                    (stack, world, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("StoneBitmask")) {
                            return stack.getTag().getInt("StoneBitmask");
                        }
                        return 0F;
                    }
            );
            ItemProperties.register(
                    ModItems.SOUL_STONE_HOLDER.get(),
                    new ResourceLocation(egm.MODID, "stone_bitmask"),
                    (stack, world, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("StoneBitmask")) {
                            return stack.getTag().getInt("StoneBitmask");
                        }
                        return 0F;
                    }
            );
            ItemProperties.register(
                    ModItems.TIME_STONE_HOLDER.get(),
                    new ResourceLocation(egm.MODID, "stone_bitmask"),
                    (stack, world, entity, seed) -> {
                        if (stack.hasTag() && stack.getTag().contains("StoneBitmask")) {
                            return stack.getTag().getInt("StoneBitmask");
                        }
                        return 0F;
                    }
            );
        });
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public class ModEntityRenderers {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.SINGULARITY.get(), SingularityRenderer::new);
            event.registerEntityRenderer(ModEntities.METEOR.get(), MeteorRenderer::new);
        }
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(final RegisterParticleProvidersEvent event) {
        ClientParticles.register(event);
    }
}
