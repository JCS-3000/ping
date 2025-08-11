package org.jcs.egm.gauntlet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityRegistries;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InfinityGauntletItem extends Item {

    private static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("c460a0f0-5bf3-4bc1-924a-6e1cfacb17f7");
    private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("99c47397-8eeb-428e-b5db-75a3976d83e3");

    public InfinityGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        if (slot == EquipmentSlot.MAINHAND) {
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 8.0D, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", 2.4D, AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int idx = getSelectedStone(stack);
        String stoneKey = getSelectedStoneName(stack);
        ItemStackHandler handler = new ItemStackHandler(6);
        if (stack.hasTag() && stack.getTag().contains("Stones")) {
            handler.deserializeNBT(stack.getTag().getCompound("Stones"));
        }
        ItemStack stoneStack = handler.getStackInSlot(idx);
        if (!stoneStack.isEmpty()) {
            IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(stoneKey, stoneStack);
            if (ability == null) return InteractionResultHolder.pass(stack);

            // Universal guard: blocks if on cooldown (container-independent) and re-syncs overlay to gauntlet.
            if (StoneAbilityCooldowns.guardUse(player, stoneStack, stoneKey, ability)) {
                return InteractionResultHolder.pass(stack);
            }

            if (ability.canHoldUse()) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            } else if (!level.isClientSide) {
                ability.activate(level, player, stoneStack);
                // Apply container-aware cooldown + player gate using the STONE stack
                StoneAbilityCooldowns.apply(player, stoneStack, stoneKey, ability);
                // Make sure the stone NBT is put back (if mutated) and bitmask is updated
                handler.setStackInSlot(idx, stoneStack);
                stack.getTag().put("Stones", handler.serializeNBT());
                updateStonesBitmaskNBT(stack);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            ItemStackHandler handler = new ItemStackHandler(6);
            if (stack.hasTag() && stack.getTag().contains("Stones")) {
                handler.deserializeNBT(stack.getTag().getCompound("Stones"));
            }
            int idx = getSelectedStone(stack);
            if (idx < 0 || idx >= handler.getSlots()) {
                idx = 0;
            }

            ItemStack stoneStack = handler.getStackInSlot(idx);
            if (!stoneStack.isEmpty()) {
                String stoneKey = getSelectedStoneName(stack);
                IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(stoneKey, stoneStack);
                if (ability != null && ability.canHoldUse()) {
                    ability.onUsingTick(level, player, stoneStack, count);
                    handler.setStackInSlot(idx, stoneStack);
                    stack.getTag().put("Stones", handler.serializeNBT());
                }
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            ItemStackHandler handler = new ItemStackHandler(6);
            if (stack.hasTag() && stack.getTag().contains("Stones")) {
                handler.deserializeNBT(stack.getTag().getCompound("Stones"));
            }
            int idx = getSelectedStone(stack);
            if (idx < 0 || idx >= handler.getSlots()) {
                idx = 0;
            }

            ItemStack stoneStack = handler.getStackInSlot(idx);
            if (!stoneStack.isEmpty()) {
                String stoneKey = getSelectedStoneName(stack);
                IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(stoneKey, stoneStack);
                if (ability != null && ability.canHoldUse()) {
                    ability.releaseUsing(level, player, stoneStack, timeLeft);
                    // Apply container-aware cooldown + player gate at the moment the hold ability "fires"/releases
                    StoneAbilityCooldowns.apply(player, stoneStack, stoneKey, ability);
                    handler.setStackInSlot(idx, stoneStack);
                    stack.getTag().put("Stones", handler.serializeNBT());
                    updateStonesBitmaskNBT(stack);
                }
            }
        }
    }
    // --------------------------------------------------------------------

    /** Helper to set a stone into a slot and automatically transfer any remaining cooldown overlay to this gauntlet. */
    public static void setStoneStack(ItemStack gauntlet, int slot, ItemStack stone, Player actor) {
        ItemStackHandler handler = new ItemStackHandler(6);
        if (gauntlet.hasTag() && gauntlet.getTag().contains("Stones")) {
            handler.deserializeNBT(gauntlet.getTag().getCompound("Stones"));
        }
        handler.setStackInSlot(slot, stone);
        gauntlet.getOrCreateTag().put("Stones", handler.serializeNBT());
        updateStonesBitmaskNBT(gauntlet);

        if (actor != null) {
            StoneAbilityCooldowns.transferOnInsert(actor, gauntlet);
        }
    }

    public static ItemStack getStoneStack(ItemStack gauntlet, int slot) {
        ItemStackHandler handler = new ItemStackHandler(6);
        if (gauntlet.hasTag() && gauntlet.getTag().contains("Stones")) {
            handler.deserializeNBT(gauntlet.getTag().getCompound("Stones"));
        }
        return handler.getStackInSlot(slot);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public static int getSelectedStone(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("SelectedStone")) {
            return stack.getTag().getInt("SelectedStone");
        }
        return 0; // default to slot 0 if not present
    }

    public static void setSelectedStone(ItemStack stack, int index) {
        stack.getOrCreateTag().putInt("SelectedStone", index);
    }

    public static String getSelectedStoneName(ItemStack stack) {
        int idx = getSelectedStone(stack);
        return switch (idx) {
            case 0 -> "time";
            case 1 -> "power";
            case 2 -> "space";
            case 3 -> "reality";
            case 4 -> "soul";
            case 5 -> "mind";
            default -> "none";
        };
    }

    public static boolean hasAllStones(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(6);
        if (stack.hasTag() && stack.getTag().contains("Stones")) {
            handler.deserializeNBT(stack.getTag().getCompound("Stones"));
        }
        return !handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(0).getItem() == ModItems.TIME_STONE.get() &&
                !handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(1).getItem() == ModItems.POWER_STONE.get() &&
                !handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(2).getItem() == ModItems.SPACE_STONE.get() &&
                !handler.getStackInSlot(3).isEmpty() && handler.getStackInSlot(3).getItem() == ModItems.REALITY_STONE.get() &&
                !handler.getStackInSlot(4).isEmpty() && handler.getStackInSlot(4).getItem() == ModItems.SOUL_STONE.get() &&
                !handler.getStackInSlot(5).isEmpty() && handler.getStackInSlot(5).getItem() == ModItems.MIND_STONE.get();
    }

    public static int getStonesBitmask(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(6);
        if (stack.hasTag() && stack.getTag().contains("Stones")) {
            handler.deserializeNBT(stack.getTag().getCompound("Stones"));
        }
        int bitmask = 0;
        if (!handler.getStackInSlot(0).isEmpty()) bitmask |= 32;      // Time
        if (!handler.getStackInSlot(1).isEmpty()) bitmask |= 16;      // Power
        if (!handler.getStackInSlot(2).isEmpty()) bitmask |= 8;       // Space
        if (!handler.getStackInSlot(3).isEmpty()) bitmask |= 4;       // Reality
        if (!handler.getStackInSlot(4).isEmpty()) bitmask |= 2;       // Soul
        if (!handler.getStackInSlot(5).isEmpty()) bitmask |= 1;       // Mind
        return bitmask;
    }

    /** Call this every time the Stones handler is changed! */
    public static void updateStonesBitmaskNBT(ItemStack stack) {
        int bitmask = getStonesBitmask(stack);
        stack.getOrCreateTag().putInt("StoneBitmask", bitmask);
    }
}
