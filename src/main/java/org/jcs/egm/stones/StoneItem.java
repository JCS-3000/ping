package org.jcs.egm.stones;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class StoneItem extends Item {
    private final String key;
    private final int nameColor;

    /**
     * @param key    Stone identifier (e.g., "mind", "power")
     * @param color  RGB hex color for the item name (e.g., 0xffdd00)
     * @param properties Item properties
     */
    public StoneItem(String key, int color, Properties properties) {
        super(properties.stacksTo(1));
        this.key = key;
        this.nameColor = color;
    }

    public String getKey() {
        return this.key;
    }

    // --- FIX: Add a public getter for the color, for overlays and rendering ---
    public int getColor() {
        return this.nameColor;
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        MutableComponent base = Component.translatable(this.getDescriptionId(stack));
        return base.withStyle(style -> style.withColor(TextColor.fromRgb(nameColor)));
    }

    @Override public UseAnim getUseAnimation(ItemStack stack)   { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack stack)         { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        IGStoneAbility ability = StoneAbilities.REGISTRY.get(key);

        if (ability == null) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("No ability for " + key));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (ability.canHoldUse()) {
            player.startUsingItem(hand);
        }
        if (!level.isClientSide) {
            ability.activate(level, player, stack);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player && !level.isClientSide) {
            IGStoneAbility ability = StoneAbilities.REGISTRY.get(key);
            if (ability != null && ability.canHoldUse()) {
                ability.onUsingTick(level, player, stack, count);
            }
        }
    }

    // --- Add this override to trigger cleanup on use stop ---
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player && !level.isClientSide) {
            IGStoneAbility ability = StoneAbilities.REGISTRY.get(key);
            if (ability != null && ability.canHoldUse()) {
                ability.releaseUsing(level, player, stack, timeLeft);
            }
        }
    }


    public boolean canHoldUse() {
        IGStoneAbility ability = StoneAbilities.REGISTRY.get(key);
        return ability != null && ability.canHoldUse();
    }

    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        IGStoneAbility ability = StoneAbilities.REGISTRY.get(key);
        if (ability != null && ability.canHoldUse()) {
            ability.onUsingTick(level, player, stack, count);
        }
    }
}
