package org.jcs.egm.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.client.particle.UniversalTintParticle;
import org.jcs.egm.egm;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, egm.MODID);
    public static final RegistryObject<SimpleParticleType> REALITY_STONE_EFFECT_ONE =
            PARTICLES.register("reality_stone_effect_one", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> POWER_STONE_EFFECT_ONE =
            PARTICLES.register("power_stone_effect_one", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> POWER_STONE_EFFECT_TWO =
            PARTICLES.register("power_stone_effect_two", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> TIME_STONE_EFFECT_ONE =
            PARTICLES.register("time_stone_effect_one", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SOUL_STONE_EFFECT_ONE =
            PARTICLES.register("soul_stone_effect_one", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> UNIVERSAL_PARTICLE_ONE =
            PARTICLES.register("universal_particle_one", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> UNIVERSAL_PARTICLE_TWO =
            PARTICLES.register("universal_particle_two", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> UNIVERSAL_PARTICLE_THREE =
            PARTICLES.register("universal_particle_three", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> UNIVERSAL_PARTICLE_FOUR =
            PARTICLES.register("universal_particle_four", () -> new SimpleParticleType(false));

}

