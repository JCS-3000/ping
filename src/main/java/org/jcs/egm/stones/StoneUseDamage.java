package org.jcs.egm.stones;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class StoneUseDamage {
    public static final ResourceKey<DamageType> INFINITY_STONE_KEY =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("egm", "infinity_stone"));

    public static DamageSource get(Level level, LivingEntity entity) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(INFINITY_STONE_KEY);
        return new DamageSource(type, entity);
    }
}
