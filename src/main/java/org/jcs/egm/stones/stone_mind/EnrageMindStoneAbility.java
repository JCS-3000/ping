package org.jcs.egm.stones.stone_mind;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EnrageMindStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "enrage"; }

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        Item cdItem = StoneAbilityCooldowns.pickCooldownItem(player, stack);
        if (StoneAbilityCooldowns.isCooling(player, cdItem)) return;

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10));
        if (mobs.isEmpty()) {
            return;
        }

        for (Mob mob : mobs) {
            int duration = 20 * 60 * 5;
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1));

            Optional<Player> nearest = level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(50))
                    .stream()
                    .filter(p -> !p.getUUID().equals(player.getUUID()))
                    .min(Comparator.comparingDouble(p -> p.distanceTo(mob)));

            if (nearest.isPresent()) {
                mob.setTarget(nearest.get());
            } else {
                Optional<LivingEntity> other = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16))
                        .stream()
                        .filter(e -> e != mob && !(e instanceof Player))
                        .min(Comparator.comparingDouble(e -> e.distanceTo(mob)));
                other.ifPresent(mob::setTarget);
            }
        }

        StoneAbilityCooldowns.apply(player, cdItem, "mind", abilityKey());
    }

    @Override
    public boolean canHoldUse() { return false; }
}
