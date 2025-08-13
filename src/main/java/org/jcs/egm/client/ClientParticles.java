package org.jcs.egm.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.client.particle.*;
import org.jcs.egm.registry.ModParticles;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientParticles {
    private ClientParticles() {}

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.REALITY_STONE_EFFECT_ONE.get(), RealityStoneEffectOne.Provider::new);
        event.registerSpriteSet(ModParticles.POWER_STONE_EFFECT_ONE.get(), PowerStoneEffectOne.Provider::new);
        event.registerSpriteSet(ModParticles.POWER_STONE_EFFECT_TWO.get(), PowerStoneEffectOne.Provider::new);


        event.registerSpriteSet(ModParticles.CHARGING_PARTICLE_SPACE.get(), ChargingParticle.Provider::new);
        event.registerSpriteSet(ModParticles.CHARGING_PARTICLE_POWER.get(), ChargingParticle.Provider::new);
        // Time
        event.registerSpriteSet(ModParticles.TIME_STONE_EFFECT_ONE.get(), TimeStoneEffectOne.Provider::new);
        // Soul
        event.registerSpriteSet(ModParticles.SOUL_STONE_EFFECT_ONE.get(), SoulStoneEffectOne.Provider::new);
        // UNIVERSAL
        event.registerSpriteSet(ModParticles.UNIVERSAL_PARTICLE_ONE.get(), org.jcs.egm.client.particle.UniversalTintParticle.Provider::new);
        event.registerSpriteSet(ModParticles.UNIVERSAL_PARTICLE_TWO.get(), org.jcs.egm.client.particle.UniversalTintParticle.Provider::new);
        event.registerSpriteSet(ModParticles.UNIVERSAL_PARTICLE_THREE.get(), org.jcs.egm.client.particle.UniversalTintParticle.Provider::new);
        event.registerSpriteSet(ModParticles.UNIVERSAL_PARTICLE_FOUR.get(), org.jcs.egm.client.particle.UniversalTintParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        register(event);
    }
}
