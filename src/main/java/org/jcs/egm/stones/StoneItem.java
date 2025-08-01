package org.jcs.egm.stones;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jcs.egm.client.StoneAbilityMenuScreen;

import java.util.List;

public class StoneItem extends Item {
    private final String key;
    private final int nameColor;

    public StoneItem(String key, int color, Properties properties) {
        super(properties.stacksTo(1));
        this.key = key;
        this.nameColor = color;
    }

    public String getKey() {
        return this.key;
    }

    public int getColor() {
        return this.nameColor;
    }

    public boolean canHoldUse(ItemStack stack) {
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.getKey(), stack);
        return ability != null && ability.canHoldUse();
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        MutableComponent base = Component.translatable(this.getDescriptionId(stack));
        return base.withStyle(style -> style.withColor(TextColor.fromRgb(nameColor)));
    }

    @Override public UseAnim getUseAnimation(ItemStack stack)   { return UseAnim.BOW; }

    @Override public int getUseDuration(ItemStack stack)         { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack usedStack = player.getItemInHand(hand);
        int idx = usedStack.getOrCreateTag().getInt("AbilityIndex");
        System.out.println("[SERVER] Ability activation for " + usedStack + "; index: " + idx + "; isClient=" + world.isClientSide);
        IGStoneAbility ability = StoneAbilityRegistries.getSelectedAbility(this.getKey(), usedStack);
        if (ability != null && ability.canHoldUse()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(usedStack);
        } else if (ability != null && !world.isClientSide) {
            ability.activate(world, player, usedStack);
            return InteractionResultHolder.success(usedStack);
        }
        return InteractionResultHolder.pass(usedStack);
    }

    // Open ability selection menu client-side when left-clicking an entity
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (player.level().isClientSide) {
            openStoneAbilityMenu(stack, InteractionHand.MAIN_HAND, this.getKey());
        }
        return true;
    }

    /** Helper to open the StoneAbilityMenuScreen client-side */
    public static void openStoneAbilityMenu(ItemStack stack, InteractionHand hand, String stoneKey) {
        if (Minecraft.getInstance().player == null) return;

        List<Component> abilityNames = StoneAbilityRegistries.getAbilityNames(stoneKey);

        int selectedIndex = stack.getOrCreateTag().getInt("AbilityIndex");
        Minecraft.getInstance().setScreen(
                new StoneAbilityMenuScreen(stack, hand, abilityNames, selectedIndex)
        );
    }
}
