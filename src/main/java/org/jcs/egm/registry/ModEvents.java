package org.jcs.egm.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.command.SoulRealmCommand;
import org.jcs.egm.command.SoulStoneRezResetCommand;
import org.jcs.egm.command.ToggleCooldownsCommand;
import org.jcs.egm.egm;
import org.jcs.egm.registry.ModEffects;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.StoneAbilityRegistries;
import org.jcs.egm.stones.stone_power.EmpoweredPunchPowerStoneAbility;
import org.jcs.egm.stones.stone_power.PowerStoneItem;

@Mod.EventBusSubscriber(modid = egm.MODID)
public class ModEvents {
    private static final SoundEvent POWER_PUNCH_SOUND = SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "power_punch"));
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SoulRealmCommand.register(event.getDispatcher());
        SoulStoneRezResetCommand.register(event.getDispatcher());
        ToggleCooldownsCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onAnvilTransformation(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack heldItem = player.getItemInHand(event.getHand());
        BlockState blockState = level.getBlockState(pos);

        if (player.isShiftKeyDown() && 
            heldItem.is(Items.NETHER_STAR) && 
            blockState.getBlock() instanceof AnvilBlock) {
            
            if (!level.isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) level;
                
                level.setBlock(pos, ModBlocks.NIDAVELLIRIAN_FORGE.get().defaultBlockState()
                    .setValue(org.jcs.egm.blocks.NidavelliranForgeBlock.FACING, 
                             blockState.getValue(AnvilBlock.FACING)), 3);
                
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                if (lightning != null) {
                    lightning.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(lightning);
                }
                
                for (int i = 0; i < 50; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
                    double offsetY = level.random.nextDouble() * 2.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
                    
                    serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + 0.5 + offsetY,
                        pos.getZ() + 0.5 + offsetZ,
                        1, 0, 0, 0, 0.1);
                }
                
                level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
                
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
            }
            
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        
        // Check if player has empowered punch effect
        if (player.hasEffect(ModEffects.EMPOWERED_PUNCH.get()) && target instanceof LivingEntity livingTarget) {
            Level level = player.level();
            
            // Remove the effect immediately after use
            player.removeEffect(ModEffects.EMPOWERED_PUNCH.get());
            
            // Deal enhanced damage (18 damage total)
            DamageSource damageSource = level.damageSources().playerAttack(player);
            livingTarget.hurt(damageSource, 18.0F);
            
            // Calculate knockback direction
            Vec3 playerPos = player.position();
            Vec3 targetPos = livingTarget.position();
            Vec3 direction = targetPos.subtract(playerPos).normalize();
            
            // Apply strong knockback
            Vec3 knockback = direction.scale(3.0).add(0, 1.5, 0);
            livingTarget.push(knockback.x, knockback.y, knockback.z);
            
            // Particle effects at impact
            if (level instanceof ServerLevel serverLevel) {
                Vec3 impactPos = livingTarget.position().add(0, livingTarget.getBbHeight() / 2.0, 0);
                serverLevel.sendParticles(ModParticles.POWER_STONE_EFFECT_ONE.get(),
                        impactPos.x, impactPos.y, impactPos.z, 30, 0.5, 0.5, 0.5, 0.2);
            }
            
            // Play power punch sound
            level.playSound(null, livingTarget.blockPosition(), POWER_PUNCH_SOUND, 
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            
            // Cancel the normal attack to prevent double damage
            event.setCanceled(true);
        }
    }
}
