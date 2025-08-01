package org.jcs.egm.stones.stone_mind;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.holders.StoneHolderItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class MindStonePassiveHandler {
    // Track time since last XP given for each player
    private static final Map<UUID, Integer> tickCounters = new HashMap<>();
    // XP drip configuration (see explanation below)
    private static final int XP_PER_HOUR = 1395;
    private static final int TICKS_PER_XP = (20 * 60 * 60) / XP_PER_HOUR; // ~52 ticks per XP

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isSpectator() || player.isCreative()) return;

        // Only active if player has Mind Stone, Mind Stone Holder (Lokian Scepter) w/ Mind Stone, or Gauntlet+Stone
        boolean found = false;
        for (ItemStack stack : player.getInventory().items) {
            // Raw Mind Stone or Gauntlet+Stone:
            if (stack.is(ModItems.MIND_STONE.get()) || IGStoneAbility.isInGauntlet(player, stack)) {
                found = true;
                break;
            }
            // Mind Stone Holder (Lokian Scepter) containing Mind Stone:
            if (stack.is(ModItems.MIND_STONE_HOLDER.get())) {
                ItemStack inside = StoneHolderItem.getStone(stack);
                if (inside != null && inside.is(ModItems.MIND_STONE.get())) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            tickCounters.remove(player.getUUID());
            return;
        }

        // Tick counter for this player
        UUID uuid = player.getUUID();
        int ticks = tickCounters.getOrDefault(uuid, 0) + 1;
        if (ticks >= TICKS_PER_XP) {
            player.giveExperiencePoints(1);
            ticks = 0;
        }
        tickCounters.put(uuid, ticks);
    }
}
