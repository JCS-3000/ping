package org.jcs.egm.stones.stone_mind;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jcs.egm.holders.StoneHolderItem;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MindStoneItem extends StoneItem {
    @Override
    public String getKey() { return "mind"; }
    @Override
    public int getColor() { return 0xffdd00; }

    private static final Map<UUID, Integer> tickCounters = new HashMap<>();
    private static final int XP_PER_HOUR = 1395;
    private static final int TICKS_PER_XP = (20 * 60 * 60) / XP_PER_HOUR; // ~52 ticks per XP

    public MindStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = MindStoneAbilityRegistry.getSelectedAbility(stack);
        if (ability == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        } else if (!world.isClientSide) {
            ability.activate(world, player, stack);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    // Passive XP drip logic as static subscriber
    @Mod.EventBusSubscriber
    public static class PassiveHandler {
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.player instanceof ServerPlayer player)) return;
            if (player.isSpectator() || player.isCreative()) return;

            // Only active if player has Mind Stone, Mind Stone Holder, or Gauntlet+Stone
            boolean found = false;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof MindStoneItem || StoneItem.isInGauntlet(player, stack)) {
                    found = true;
                    break;
                }
                if (stack.is(ModItems.MIND_STONE_HOLDER.get())) {
                    ItemStack inside = StoneHolderItem.getStone(stack);
                    if (inside != null && inside.getItem() instanceof MindStoneItem) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                tickCounters.remove(player.getUUID());
                return;
            }
            UUID uuid = player.getUUID();
            int ticks = tickCounters.getOrDefault(uuid, 0) + 1;
            if (ticks >= TICKS_PER_XP) {
                player.giveExperiencePoints(1);
                ticks = 0;
            }
            tickCounters.put(uuid, ticks);
        }
    }
}
