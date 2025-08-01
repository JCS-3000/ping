package org.jcs.egm.stones.effects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.stones.StoneItem;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StoneHoldDamage {

    private static int tickCounter = 0;

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            Level level = player.level();
            if (level.isClientSide) return; // Only run on server side

            tickCounter++;
            if (tickCounter % 40 != 0) return; // Every 2 seconds

            // Only apply if holding a stone and NOT holding the gauntlet
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();

            // Not holding gauntlet in either hand?
            boolean hasGauntlet = main.getItem() instanceof InfinityGauntletItem || off.getItem() instanceof InfinityGauntletItem;
            if (hasGauntlet) return;

            // Is holding a stone in either hand?
            if (main.getItem() instanceof StoneItem || off.getItem() instanceof StoneItem) {
                player.hurt(StoneUseDamage.get(level, player), 1.0F);
            }
        }
    }
