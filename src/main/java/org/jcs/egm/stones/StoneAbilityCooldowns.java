package org.jcs.egm.stones;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Central cooldown registry for all Infinity Stone abilities.
 * Cooldown values are in ticks (20 ticks = 1 second).
 * Modify values here to adjust across stones.
 */
public final class StoneAbilityCooldowns {

    private static final Map<String, Integer> COOLDOWNS = new HashMap<>();

    static {
        // ===== POWER STONE =====
        put("power", "shockwave_slam",       60);  // 3s
        put("power", "infinite_lightning",  160);  // 8s

        // ===== SOUL STONE =====
        put("soul", "soul_bind",            120);  // 6s

        // ===== SPACE STONE =====
        put("space", "enderic_beam",         40);  // 2s

        // ===== REALITY STONE =====
        put("reality", "metamorphosis",      40);  // 2s (placeholder if you add cooldown later)

        // Add others here later as needed
    }

    private static void put(String stoneKey, String abilityKey, int ticks) {
        COOLDOWNS.put((stoneKey + ":" + abilityKey).toLowerCase(Locale.ROOT), ticks);
    }

    public static int getCooldown(String stoneKey, String abilityKey) {
        return COOLDOWNS.getOrDefault((stoneKey + ":" + abilityKey).toLowerCase(Locale.ROOT), 0);
    }

    public static boolean isCooling(Player player, Item item) {
        return player.getCooldowns().isOnCooldown(item);
    }

    public static void apply(Player player, Item cooldownItem, String stoneKey, String abilityKey) {
        int ticks = getCooldown(stoneKey, abilityKey);
        if (ticks > 0) {
            player.getCooldowns().addCooldown(cooldownItem, ticks);
        }
    }

    public static void apply(Player player, Item cooldownItem, String stoneKey, IGStoneAbility ability) {
        apply(player, cooldownItem, stoneKey, ability.abilityKey());
    }

    public static void applyFromStack(Player player, ItemStack stack, String stoneKey, IGStoneAbility ability) {
        apply(player, pickCooldownItem(player, stack), stoneKey, ability.abilityKey());
    }

    public static Item pickCooldownItem(Player player, ItemStack fallback) {
        ItemStack using = player.getUseItem();
        if (!using.isEmpty()) return using.getItem();
        if (fallback != null && !fallback.isEmpty()) return fallback.getItem();
        return player.getMainHandItem().getItem();
    }
}
