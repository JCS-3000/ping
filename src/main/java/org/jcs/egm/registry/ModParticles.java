package org.jcs.egm.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.egm;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, egm.MODID);

    public static final RegistryObject<SimpleParticleType> REALITY_STONE_EFFECT_ONE =
            PARTICLES.register("reality_stone_effect_one", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> POWER_STONE_EFFECT_ONE =
            PARTICLES.register("power_stone_effect_one", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> TIME_STONE_EFFECT_ONE =
            PARTICLES.register("time_stone_effect_one", () -> new SimpleParticleType(true));
}
