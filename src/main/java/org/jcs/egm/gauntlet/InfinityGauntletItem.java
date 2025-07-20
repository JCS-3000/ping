package org.jcs.egm.gauntlet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;
import org.jcs.egm.registry.ModItems;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilities;
import org.jcs.egm.stones.StoneItem;
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

        // Open GUI on shift + right-click
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(
                        sp,
                        new InfinityGauntletMenu.Provider(stack),
                        buf -> buf.writeItem(stack)
                );
            }
            return InteractionResultHolder.success(stack);
        }

        // Load gauntlet inventory from NBT
        ItemStackHandler handler = new ItemStackHandler(6);
        if (stack.hasTag() && stack.getTag().contains("Stones")) {
            handler.deserializeNBT(stack.getTag().getCompound("Stones"));
        }

        // Get selected stone index and clamp it
        int idx = getSelectedStone(stack);
        int maxSlots = handler.getSlots();
        if (idx < 0 || idx >= maxSlots) {
            idx = 0;
            setSelectedStone(stack, 0);
        }

        ItemStack stoneStack = handler.getStackInSlot(idx);

        if (!stoneStack.isEmpty() && stoneStack.getItem() instanceof StoneItem stoneItem) {
            IGStoneAbility ability = StoneAbilities.REGISTRY.get(stoneItem.getKey());
            if (ability != null) {
                if (stoneItem.canHoldUse()) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(stack);
                } else if (!level.isClientSide) {
                    ability.activate(level, player, stoneStack);
                }
            }
        } else if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("No Infinity Stone in the selected slot."), true);
        }

        return InteractionResultHolder.success(stack);
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
            if (!stoneStack.isEmpty() && stoneStack.getItem() instanceof StoneItem stoneItem) {
                IGStoneAbility ability = org.jcs.egm.stones.StoneAbilities.REGISTRY.get(stoneItem.getKey());
                if (ability != null && stoneItem.canHoldUse()) {
                    ability.onUsingTick(level, player, stoneStack, count);
                }
            }
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
        return UseAnim.BLOCK; // BLOCK for shields, BOW for bow-pulling
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
        if (!handler.getStackInSlot(2).isEmpty()) bitmask |= 8;      // Space
        if (!handler.getStackInSlot(3).isEmpty()) bitmask |= 4;      // Reality
        if (!handler.getStackInSlot(4).isEmpty()) bitmask |= 2;     // Soul
        if (!handler.getStackInSlot(5).isEmpty()) bitmask |= 1;     // Mind
        return bitmask;
    }

    // --- ADD THIS METHOD FOR MODEL OVERRIDE ---
    /** Call this every time the Stones handler is changed! */
    public static void updateStonesBitmaskNBT(ItemStack stack) {
        int bitmask = getStonesBitmask(stack);
        stack.getOrCreateTag().putInt("StoneBitmask", bitmask);

    }
}
