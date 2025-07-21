package org.jcs.egm.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.egm;
import org.jcs.egm.entity.PowerStoneLightningEntity;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, egm.MODID);

    public static final RegistryObject<EntityType<PowerStoneLightningEntity>> POWER_STONE_LIGHTNING =
            ENTITIES.register("power_stone_lightning",
                    () -> EntityType.Builder.<PowerStoneLightningEntity>of(PowerStoneLightningEntity::new, MobCategory.MISC)
                            .sized(0.2f, 0.2f)
                            .clientTrackingRange(128)
                            .build("power_stone_lightning"));


}
