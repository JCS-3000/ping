package org.jcs.egm.stones.stone_soul;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jcs.egm.stones.IGStoneAbility;


import java.util.List;

public class SoulStoneAbility implements IGStoneAbility {

    @Override
    public void activate(Level level, Player player, ItemStack gauntletStack) {
        // Only run on server side
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Define a 5×5×4 area (2.5 blocks out, 1 down, 3 up)
        AABB area = new AABB(
                player.getX() - 2.5, player.getY() - 1.0, player.getZ() - 2.5,
                player.getX() + 2.5, player.getY() + 3.0, player.getZ() + 2.5
        );

        List<ZombieVillager> zombies = serverLevel.getEntitiesOfClass(ZombieVillager.class, area);
        for (ZombieVillager zombie : zombies) {
            // Create a new Villager at the zombie's position & rotation
            Villager villager = EntityType.VILLAGER.create(serverLevel);
            if (villager == null) continue;

            villager.moveTo(
                    zombie.getX(), zombie.getY(), zombie.getZ(),
                    zombie.getYRot(), zombie.getXRot()
            );
            serverLevel.addFreshEntity(villager);

            // Spawn heart particles at the former zombie
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    zombie.getX(), zombie.getY() + 1.0, zombie.getZ(),
                    8,      // count
                    0.5,    // dx
                    0.5,    // dy
                    0.5,    // dz
                    0.0     // speed
            );

            // Play the villager approval sound
            serverLevel.playSound(
                    null,
                    zombie.blockPosition(),
                    SoundEvents.VILLAGER_YES,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            // Remove the zombie villager
            zombie.discard();
        }
    }
}
