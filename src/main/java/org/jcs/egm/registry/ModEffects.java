package org.jcs.egm.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.egm;
import org.jcs.egm.effects.EmpoweredPunchEffect;
import org.jcs.egm.effects.StoneSicknessEffect;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, egm.MODID);

    public static final RegistryObject<MobEffect> EMPOWERED_PUNCH =
            EFFECTS.register("empowered_punch", EmpoweredPunchEffect::new);

    public static final RegistryObject<MobEffect> STONE_SICKNESS =
            EFFECTS.register("stone_sickness", StoneSicknessEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
