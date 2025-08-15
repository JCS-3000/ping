package org.jcs.egm.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class EmpoweredPunchEffect extends MobEffect {
    
    public EmpoweredPunchEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8000FF); // Purple color for power stone
    }

    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier) {
        // This effect doesn't do anything over time, it's just a marker
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // No ticking effects
    }
}