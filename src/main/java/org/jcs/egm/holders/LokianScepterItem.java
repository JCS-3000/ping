package org.jcs.egm.holders;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityRegistries;
import org.jcs.egm.stones.StoneAbilityCooldowns;

public class LokianScepterItem extends SwordItem {
    private static final String STONE_TYPE = "mind";

    // Custom tier for Lokian Scepter
    private static final Tier LOKIAN_TIER = new Tier() {
        @Override
        public int getUses() { return 1500; }
        
        @Override
        public float getSpeed() { return 6.0f; }
        
        @Override
        public float getAttackDamageBonus() { return 0.0f; } // +2 base damage = 2 total
        
        @Override
        public int getLevel() { return 2; }
        
        @Override
        public int getEnchantmentValue() { return 20; }
        
        @Override
        public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    };

    public LokianScepterItem(Properties properties) {
        super(LOKIAN_TIER, 2, -1.0f, properties); // -1.0f = 3.0 attack speed
    }

    public String getStoneKey() {
        return STONE_TYPE;
    }

    /** Extracts the single stone inside */
    public static ItemStack getStone(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStackHandler handler = new ItemStackHandler(1);
        if (stack.hasTag() && stack.getTag().contains("Stone")) {
            handler.deserializeNBT(stack.getTag().getCompound("Stone"));
        }
        return handler.getStackInSlot(0);
    }

    /** Puts a (possibly mutated) stone back and updates model predicate bitmask */
    public static void setStone(ItemStack holder, ItemStack inside) {
        setStone(holder, inside, null);
    }

    /** Same as setStone(holder, inside) but also transfers cooldown overlay if a player context is available */
    public static void setStone(ItemStack holder, ItemStack inside, Player actor) {
        if (holder == null || holder.isEmpty()) return;
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, inside);
        if (!holder.hasTag()) holder.setTag(new CompoundTag());
        holder.getTag().put("Stone", handler.serializeNBT());
        updateStoneBitmaskNBT(holder);

        // If this write is due to an insertion by a player, re-sync any remaining cooldown overlay to this holder.
        if (actor != null) {
            StoneAbilityCooldowns.transferOnInsert(actor, holder);
        }
    }

    /** Returns 0 (empty) or 1 (has stone) for predicate */
    public static int getStonePresenceBit(ItemStack stack) {
        return getStone(stack).isEmpty() ? 0 : 1;
    }

    /** Writes the bit to NBT for model predicate override */
    public static void updateStoneBitmaskNBT(ItemStack stack) {
        stack.getOrCreateTag().putInt("StoneBitmask", getStonePresenceBit(stack));
    }

    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new net.minecraftforge.common.capabilities.ICapabilityProvider() {
            private final LazyOptional<IItemHandler> handler = LazyOptional.of(() ->
                    new ItemStackHandler(1) {
                        @Override
                        public boolean isItemValid(int slot, @NotNull ItemStack toInsert) {
                            return isCorrectStone(toInsert);
                        }
                    }
            );
            @Override
            public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.core.Direction side) {
                return cap == ForgeCapabilities.ITEM_HANDLER ? handler.cast() : LazyOptional.empty();
            }
        };
    }

    /** Ensures only the mind stone can go inside */
    public boolean isCorrectStone(ItemStack stack) {
        return stack.getItem() instanceof org.jcs.egm.stones.stone_mind.MindStoneItem;
    }

    public MenuProvider getMenuProvider(ItemStack stack) {
        return new SimpleMenuProvider(
                (id, inv, plyr) -> new StoneHolderMenu(id, inv, stack),
                Component.literal("Lokian Scepter")
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack holder = player.getItemInHand(hand);
        ItemStack inside = getStone(holder);
        if (inside.isEmpty()) {
            return InteractionResultHolder.pass(holder);
        }
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(STONE_TYPE, inside);
        if (ability == null) return InteractionResultHolder.pass(holder);

        // Universal guard: blocks if on cooldown (container-independent) and re-syncs overlay to this holder.
        if (StoneAbilityCooldowns.guardUse(player, inside, STONE_TYPE, ability)) {
            return InteractionResultHolder.pass(holder);
        }

        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(holder);
        } else if (!world.isClientSide) {
            ability.activate(world, player, inside);
            // Apply container-aware cooldown + player gate using the STONE stack
            StoneAbilityCooldowns.apply(player, inside, STONE_TYPE, ability);
            setStone(holder, inside, player);
            return InteractionResultHolder.success(holder);
        }
        return InteractionResultHolder.pass(holder);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level world, LivingEntity entity, ItemStack holder, int count) {
        if (!(entity instanceof Player player)) return;
        ItemStack inside = getStone(holder);
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(STONE_TYPE, inside);
        if (ability != null && ability.canHoldUse()) {
            ability.onUsingTick(world, player, inside, count);
            setStone(holder, inside, player);
        }
    }

    @Override
    public void releaseUsing(ItemStack holder, Level world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        ItemStack inside = getStone(holder);
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(STONE_TYPE, inside);
        if (ability != null && ability.canHoldUse()) {
            ability.releaseUsing(world, player, inside, timeLeft);
            // Apply container-aware cooldown + player gate at the moment the hold ability "fires"/releases
            StoneAbilityCooldowns.apply(player, inside, STONE_TYPE, ability);
            setStone(holder, inside, player);
        }
    }
}