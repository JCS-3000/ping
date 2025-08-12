package org.jcs.egm.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.egm;
import org.jcs.egm.entity.MeteorEntity;
import org.jcs.egm.entity.PowerStoneLightningEntity;
import org.jcs.egm.entity.SingularityEntity;
import org.jcs.egm.entity.TimeBubbleFieldEntity;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, egm.MODID);

    public static final RegistryObject<EntityType<PowerStoneLightningEntity>> POWER_STONE_LIGHTNING =
            ENTITIES.register("power_stone_lightning",
                    () -> EntityType.Builder.of(PowerStoneLightningEntity::new, MobCategory.MISC)
                            .sized(0.2f, 0.2f)
                            .clientTrackingRange(128)
                            .build("power_stone_lightning"));

    public static final RegistryObject<EntityType<TimeBubbleFieldEntity>> TIME_ACCEL_FIELD =
            ENTITIES.register("time_accel_field",
                    () -> EntityType.Builder.of(TimeBubbleFieldEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("time_accel_field"));

    public static final RegistryObject<EntityType<SingularityEntity>> SINGULARITY =
            ENTITIES.register("singularity",
                    () -> EntityType.Builder.<SingularityEntity>of(SingularityEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("singularity"));

    public static final RegistryObject<EntityType<MeteorEntity>> METEOR =
            ENTITIES.register("meteor",
                    () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("meteor"));

}
