package org.jcs.egm.stones.stone_space;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.entity.SingularityEntity;
import org.jcs.egm.registry.ModEntities;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

public class SingularitySpaceStoneAbility implements IGStoneAbility {

    private static final SoundEvent SINGULARITY_SOUND = 
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("egm", "space_singularity"));

    @Override
    public String abilityKey() { return "singularity"; }

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;
        
        // Guard against cooldown usage
        if (StoneAbilityCooldowns.guardUse(player, stack, "space", this)) return;

        // Get target location where player is looking
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVector = player.getLookAngle();
        double range = 20.0;
        
        var hit = player.pick(range, 1.0F, false);
        Vec3 singularityPos = switch (hit.getType()) {
            case BLOCK, ENTITY -> hit.getLocation().add(0, 1, 0); // Slightly above hit point
            default -> eyePos.add(lookVector.scale(range));
        };

        if (level instanceof ServerLevel server) {
            // Play creation sound
            level.playSound(null, player.blockPosition(), SINGULARITY_SOUND, SoundSource.PLAYERS, 1.0F, 0.8F);
            
            // Create the singularity entity
            SingularityEntity singularity = new SingularityEntity(ModEntities.SINGULARITY.get(), server, singularityPos, player.getUUID());
            server.addFreshEntity(singularity);
        }

        StoneAbilityCooldowns.apply(player, stack, "space", this);
    }

    @Override
    public boolean canHoldUse() { return false; }
}