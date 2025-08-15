package org.jcs.egm.stones.stone_power;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.particles.ChargingParticleHelper;
import org.jcs.egm.registry.ModEffects;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EmpoweredPunchPowerStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "empowered_punch"; }

    private static final SoundEvent CHARGING_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("egm", "power_stone_charging"));
    private static final SoundEvent POWER_PUNCH_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("egm", "power_punch"));
    
    private static final Set<UUID> CHARGING_SOUND_PLAYERS = new HashSet<>();

    @Override
    public void activate(Level level, Player player, ItemStack stack) {}

    @Override
    public boolean canHoldUse() { return true; }

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        final String stone = "power";
        final String ability = abilityKey();
        int useDuration = player.getUseItem().getUseDuration();
        int ticksHeld = useDuration - count;
        int chargeTicks = StoneAbilityCooldowns.chargeup(stone, ability);
        UUID uuid = player.getUUID();

        // CHARGING PHASE
        if (ticksHeld < chargeTicks) {
            if (level.isClientSide) {
                if (ticksHeld % 4 == 0) {
                    ChargingParticleHelper.spawnPowerSuckInParticles(level, player);
                }
                if (!CHARGING_SOUND_PLAYERS.contains(uuid)) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            CHARGING_SOUND, SoundSource.PLAYERS, 0.8f, 1.0f, false);
                    CHARGING_SOUND_PLAYERS.add(uuid);
                }
            }
            return;
        }

        // FULLY CHARGED - auto-release and give effect (only once)
        if (ticksHeld == chargeTicks) {
            if (level.isClientSide) {
                if (CHARGING_SOUND_PLAYERS.contains(uuid)) {
                    Minecraft.getInstance().getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
                    CHARGING_SOUND_PLAYERS.remove(uuid);
                }
            }
            
            if (!level.isClientSide) {
                // Give player the empowered punch effect for 30 seconds
                MobEffectInstance empoweredEffect = new MobEffectInstance(
                    ModEffects.EMPOWERED_PUNCH.get(), 
                    600, // 30 seconds (20 ticks per second)
                    0,   // Amplifier 0
                    false, // Not ambient
                    true,  // Show particles
                    true   // Show icon
                );
                player.addEffect(empoweredEffect);
                
                // Send message to player
                player.displayClientMessage(
                    Component.literal("§5Gauntlet fully charged."),
                    true // Show in action bar
                );
                
                // Play XP ding sound
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, 
                        SoundSource.PLAYERS, 1.0F, 2.0F);
                
                // Apply cooldown
                StoneAbilityCooldowns.apply(player, stack, "power", this);
            }
            
            // Force stop using the item (auto-release)
            player.stopUsingItem();
        }
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int count) {
        UUID uuid = player.getUUID();

        // Clean up sound tracking
        if (level.isClientSide) {
            Minecraft.getInstance().getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
            CHARGING_SOUND_PLAYERS.remove(uuid);
        }
    }

    public void spawnChargedParticlesPublic(Level level, Player player) {
        Vec3 playerPos = player.position();
        Vec3 center = playerPos.add(0, player.getBbHeight() / 2.0, 0);
        
        // Create a swirling effect around the player while charging
        for (int i = 0; i < 6; i++) {
            double angle = (System.currentTimeMillis() / 50.0 + i * Math.PI / 3.0) % (2 * Math.PI);
            double radius = 1.2 + 0.3 * Math.sin(System.currentTimeMillis() / 100.0);
            
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + Math.sin(System.currentTimeMillis() / 80.0 + i) * 0.5;
            double z = center.z + Math.sin(angle) * radius;
            
            level.addParticle(ModParticles.POWER_STONE_EFFECT_ONE.get(), 
                    x, y, z, 0.0, 0.01, 0.0);
        }
    }
}