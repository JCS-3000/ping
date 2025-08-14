package org.jcs.egm.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.command.SoulRealmCommand;
import org.jcs.egm.command.SoulStoneRezResetCommand;
import org.jcs.egm.command.ToggleCooldownsCommand;
import org.jcs.egm.egm;

@Mod.EventBusSubscriber(modid = egm.MODID)
public class ModEvents {
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
}
