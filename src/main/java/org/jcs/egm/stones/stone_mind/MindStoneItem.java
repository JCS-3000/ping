package org.jcs.egm.stones.stone_mind;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityRegistries;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.stones.StoneItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Mind Stone:
 * - Active ability flow uses guard + container-aware cooldowns.
 * - Passive XP boost removed.
 * - NEW: Anvil catalyst — put enchanted item/book in LEFT, Mind Stone in RIGHT.
 *   For a flat cost of 30 levels, upgrades all *existing* enchants on the left item to their max level.
 *   The Mind Stone is NOT consumed (we refund it on repair via AnvilRepairEvent).
 */
public class MindStoneItem extends StoneItem {

    private static final String MAXED_MARKER = "EGM_MIND_MAXED";

    public MindStoneItem(Properties props) { super(props); }

    @Override public String getKey()   { return "mind"; }
    @Override public int    getColor() { return 0xFFD700; }

    // ===== Active ability dispatch (sanitized) ============================================

    @Override
    protected InteractionResultHolder<ItemStack> handleStoneUse(Level world, Player player, InteractionHand hand, ItemStack stack, StoneState state) {
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(getKey(), stack);
        if (ability == null) return InteractionResultHolder.pass(stack);

        if (StoneAbilityCooldowns.guardUse(player, stack, getKey(), ability)) return InteractionResultHolder.pass(stack);

        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!world.isClientSide) {
            ability.activate(world, player, stack);
            StoneAbilityCooldowns.apply(player, stack, getKey(), ability);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level world, LivingEntity entity, ItemStack stack, int count) {
        if (!(entity instanceof Player player)) return;
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(getKey(), stack);
        if (ability != null && ability.canHoldUse()) ability.onUsingTick(world, player, stack, count);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(getKey(), stack);
        if (ability != null && ability.canHoldUse()) {
            ability.releaseUsing(world, player, stack, timeLeft);
            if (!world.isClientSide) StoneAbilityCooldowns.apply(player, stack, getKey(), ability);
        }
    }

    // ===== Anvil catalyst: “Max out current enchants for 30 levels” =======================

    @Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AnvilHooks {

        @SubscribeEvent
        public static void onAnvilUpdate(AnvilUpdateEvent evt) {
            ItemStack left  = evt.getLeft();   // target: enchanted item or enchanted book
            ItemStack right = evt.getRight();  // must be the Mind Stone
            if (left.isEmpty() || right.isEmpty()) return;
            if (right.getItem() != ModItems.MIND_STONE.get()) return;

            ItemStack out;
            boolean changed = false;

            if (left.getItem() == Items.ENCHANTED_BOOK) {
                var list = EnchantedBookItem.getEnchantments(left);
                if (list.isEmpty()) return;

                out = new ItemStack(Items.ENCHANTED_BOOK);
                for (int i = 0; i < list.size(); i++) {
                    var tag = list.getCompound(i);
                    String idStr = tag.getString("id");
                    int lvl = tag.getShort("lvl");
                    Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(idStr));
                    if (ench == null) continue;
                    int max = ench.getMaxLevel();
                    if (max < 1) continue;
                    if (lvl < max) changed = true;
                    EnchantedBookItem.addEnchantment(out, new EnchantmentInstance(ench, Math.max(lvl, max)));
                }
                if (!changed) return;

            } else {
                Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(left);
                if (current.isEmpty()) return;

                Map<Enchantment, Integer> upgraded = new HashMap<>();
                for (var entry : current.entrySet()) {
                    Enchantment ench = entry.getKey();
                    int lvl = entry.getValue();
                    int max = ench.getMaxLevel();
                    int newLvl = Math.max(lvl, max);
                    if (newLvl > lvl) changed = true;
                    upgraded.put(ench, newLvl);
                }
                if (!changed) return;

                out = left.copy();
                EnchantmentHelper.setEnchantments(upgraded, out);
            }

            // Mark result so we can detect/refund in AnvilRepairEvent
            out.getOrCreateTag().putBoolean(MAXED_MARKER, true);

            // Anvil preview: cost 30 levels. We let vanilla consume 1 of the right…
            evt.setOutput(out);
            evt.setCost(30);
            evt.setMaterialCost(1); // …then we refund it below (prevents dupes on odd anvils)
        }

        @SubscribeEvent
        public static void onAnvilRepair(AnvilRepairEvent evt) {
            // Only refund if this was OUR recipe (marker present) and the right item was the Mind Stone
            ItemStack left  = evt.getLeft();
            ItemStack right = evt.getRight();
            ItemStack out   = evt.getOutput();
            if (out.isEmpty() || right.isEmpty()) return;
            if (right.getItem() != ModItems.MIND_STONE.get()) return;
            if (!out.hasTag() || !out.getTag().getBoolean(MAXED_MARKER)) return;

            // Refund exactly one Mind Stone to the player
            ItemStack refund = new ItemStack(ModItems.MIND_STONE.get());
            if (!evt.getEntity().addItem(refund)) {
                evt.getEntity().drop(refund, false);
            }

            // Optional: scrub the marker from the output (no gameplay effect either way)
            out.getTag().remove(MAXED_MARKER);
        }
    }
}
