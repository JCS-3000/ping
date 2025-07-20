package org.jcs.egm.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.entity.PowerStoneBeamEntity;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "egm");

    public static final RegistryObject<EntityType<PowerStoneBeamEntity>> POWER_STONE_BEAM = ENTITIES.register("power_stone_beam",
            () -> EntityType.Builder.<PowerStoneBeamEntity>of(PowerStoneBeamEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .build("power_stone_beam")
    );
}
