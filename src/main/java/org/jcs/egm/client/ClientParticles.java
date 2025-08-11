package org.jcs.egm.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.client.particle.UniversalTintParticle;
import org.jcs.egm.registry.ModParticles;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientParticles {
    private ClientParticles() {}

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.UNIVERSAL_PARTICLE_ONE.get(),   UniversalTintParticle.Provider::new);
        e.registerSpriteSet(ModParticles.UNIVERSAL_PARTICLE_TWO.get(),   UniversalTintParticle.Provider::new);
        e.registerSpriteSet(ModParticles.UNIVERSAL_PARTICLE_THREE.get(), UniversalTintParticle.Provider::new);
        e.registerSpriteSet(ModParticles.UNIVERSAL_PARTICLE_FOUR.get(),  UniversalTintParticle.Provider::new);
    }
}
