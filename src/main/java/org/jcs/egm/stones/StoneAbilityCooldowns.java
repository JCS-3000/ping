package org.jcs.egm.stones;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.StoneHolderItem;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Central timing registry for all Infinity Stone abilities.
 *
 * Base cooldowns are RAW (tripled so GAUNTLET ≈ old values):
 *   - RAW uses base
 *   - HOLDER uses max(base/2, 70)   // >= 3.5s
 *   - GAUNTLET uses max(base/3, 40) // >= 2.0s
 *   - 60 = 3 sec, 1200 = 1 min
 *
 * We persist cooldowns per-player (container-independent) so moving a stone
 * between raw/holder/gauntlet cannot reset the gate. We also add a vanilla
 * item cooldown to the active container for the hotbar overlay.
 */
public final class StoneAbilityCooldowns {

    private static final Map<String, Integer> COOLDOWN  = new HashMap<>();
    private static final Map<String, Integer> CHARGEUP  = new HashMap<>();
    private static final Map<String, Integer> HOLD_RATE = new HashMap<>();

    private static final String NBT_ROOT = "egm_cd";
    private static final String NBT_COOLDOWNS_DISABLED = "egm_cooldowns_disabled";
    
    private static boolean cooldownsDisabled = false;

    static {
        // ===== POWER =====
        set("power","shockwave_slam").cooldown(180).chargeup(60);
        set("power","infinite_lightning").cooldown(480).chargeup(60).holdInterval(4);
        set("power","empowered_punch").cooldown(360).chargeup(60);

        // ===== SOUL =====
        set("soul","passive_totem").cooldown(12000);
        set("soul","soul_banish").cooldown(360);

        // ===== SPACE =====
        set("space","enderic_beam").cooldown(360);
        set("space","singularity").cooldown(1000);
        set("space","moon_toss").cooldown(100).chargeup(80);

        // ===== REALITY =====
        set("reality","willed_chaos").cooldown(240);
        set("reality", "metamorphosis").cooldown(3600);

        // ===== TIME =====
        set("time","reversion").cooldown(360);
        set("time","bubble").cooldown(6000); // 5 minutes
        set("time", "freeze").cooldown(3600); // 3600 originally


        // ===== MIND =====
        set("mind","disarm").cooldown(300);
        set("mind","enrage").cooldown(360);

    }

    private StoneAbilityCooldowns() {}

    // ---------- Registry lookups (base RAW values) ----------
    public static int cooldown(String stoneKey, String abilityKey) {
        return COOLDOWN.getOrDefault(key(stoneKey, abilityKey), 0);
    }
    public static int chargeup(String stoneKey, String abilityKey) {
        return CHARGEUP.getOrDefault(key(stoneKey, abilityKey), 0);
    }
    public static int holdInterval(String stoneKey, String abilityKey) {
        return HOLD_RATE.getOrDefault(key(stoneKey, abilityKey), 0);
    }

    // ---------- Cooldown toggle management ----------
    public static boolean areCooldownsDisabled() {
        return cooldownsDisabled;
    }
    
    public static void setCooldownsDisabled(boolean disabled) {
        cooldownsDisabled = disabled;
    }
    
    public static void toggleCooldowns() {
        cooldownsDisabled = !cooldownsDisabled;
    }

    // ---------- Main apply (writes player gate + overlay on current container) ----------
    /** Preferred: pass the stone stack so we can pick the right overlay target and always set the player gate. */
    public static void apply(Player player, ItemStack stoneStack, String stoneKey, IGStoneAbility ability) {
        if (cooldownsDisabled) return; // Skip all cooldowns if disabled
        
        final String aKey = ability.abilityKey();
        final Item cdItem = pickCooldownItem(player, stoneStack);
        final int base = cooldown(stoneKey, aKey);
        final int ticks = scaleForContainer(base, cdItem);
        if (ticks <= 0) return;

        // 1) Visual overlay
        player.getCooldowns().addCooldown(cdItem, ticks);
        // 2) Container-independent gate
        setPlayerCooldown(player, stoneKey, aKey, player.level().getGameTime() + ticks);
    }

    /** Back-compat: allow existing call sites to pass the Item (overlay target). Also sets the player gate. */
    public static void apply(Player player, Item cooldownItem, String stoneKey, String abilityKey) {
        if (cooldownsDisabled) return; // Skip all cooldowns if disabled
        
        final int base = cooldown(stoneKey, abilityKey);
        final int ticks = scaleForContainer(base, cooldownItem);
        if (ticks <= 0) return;

        // 1) Visual overlay
        player.getCooldowns().addCooldown(cooldownItem, ticks);
        // 2) Container-independent gate
        setPlayerCooldown(player, stoneKey, abilityKey, player.level().getGameTime() + ticks);
    }

    /** Back-compat convenience wrapper. */
    public static void apply(Player player, Item cooldownItem, String stoneKey, IGStoneAbility ability) {
        apply(player, cooldownItem, stoneKey, ability.abilityKey());
    }

    // ---------- Use guard (block + re-sync overlay if moving between containers) ----------
    /** Return true to block use; also re-syncs overlay to the current container if cooling. */
    public static boolean guardUse(Player player, ItemStack stoneStack, String stoneKey, IGStoneAbility ability) {
        if (cooldownsDisabled) return false; // Never block if cooldowns are disabled
        
        int left = remaining(player, stoneKey, ability.abilityKey());
        if (left <= 0) return false;
        Item cdItem = pickCooldownItem(player, stoneStack);
        if (!player.getCooldowns().isOnCooldown(cdItem)) {
            player.getCooldowns().addCooldown(cdItem, left);
        }
        return true;
    }

    // ---------- “Transfer” overlay on insert into Holder/Gauntlet ----------
    public static void transferOnInsert(Player player, ItemStack containerStack) {
        if (player == null || containerStack == null || containerStack.isEmpty()) return;

        int maxRemaining = 0;

        if (containerStack.getItem() instanceof StoneHolderItem) {
            ItemStack stone = StoneHolderItem.getStone(containerStack);
            if (!stone.isEmpty() && stone.getItem() instanceof StoneItem s) {
                maxRemaining = maxRemainingFor(player, s.getKey());
            }
        } else if (containerStack.getItem() instanceof InfinityGauntletItem) {
            for (int i = 0; i < 6; i++) {
                ItemStack stone = InfinityGauntletItem.getStoneStack(containerStack, i);
                if (!stone.isEmpty() && stone.getItem() instanceof StoneItem s) {
                    maxRemaining = Math.max(maxRemaining, maxRemainingFor(player, s.getKey()));
                }
            }
        }

        if (maxRemaining > 0) {
            player.getCooldowns().addCooldown(containerStack.getItem(), maxRemaining);
        }
    }

    // ---------- Player-persistent gate ----------
    public static int remaining(Player player, String stoneKey, IGStoneAbility ability) {
        return remaining(player, stoneKey, ability.abilityKey());
    }
    public static int remaining(Player player, String stoneKey, String abilityKey) {
        long now = player.level().getGameTime();
        long end = getPlayerCooldown(player, stoneKey, abilityKey);
        long left = end - now;
        return left > 0 ? (int) left : 0;
    }

    // For transfer: longest remaining among this stone’s abilities
    public static int maxRemainingFor(Player player, String stoneKey) {
        int max = 0;
        String prefix = stoneKey.toLowerCase(Locale.ROOT) + ":";
        for (String k : COOLDOWN.keySet()) {
            if (k.startsWith(prefix)) {
                String abilityKey = k.substring(prefix.length());
                max = Math.max(max, remaining(player, stoneKey, abilityKey));
            }
        }
        return max;
    }

    // ---------- Overlay helpers ----------
    /** Vanilla item-cooldown check (used rarely; persistent gate is authoritative). */
    public static boolean isCooling(Player player, Item item) {
        return player.getCooldowns().isOnCooldown(item);
    }

    /** Choose the item to receive the vanilla overlay. */
    public static Item pickCooldownItem(Player player, ItemStack fallback) {
        ItemStack using = player.getUseItem();
        if (!using.isEmpty()) return using.getItem();

        if (fallback != null && !fallback.isEmpty()) {
            Item container = findContainerItemForStone(player, fallback);
            if (container != null) return container;
            return fallback.getItem();
        }
        return player.getMainHandItem().getItem();
    }

    // ---------- Internals ----------
    private static String key(String stoneKey, String abilityKey) {
        return (stoneKey + ":" + abilityKey).toLowerCase(Locale.ROOT);
    }
    private static Registrar set(String stoneKey, String abilityKey) {
        return new Registrar(stoneKey, abilityKey);
    }
    private static final class Registrar {
        private final String s, a;
        Registrar(String s, String a) { this.s = s; this.a = a; }
        Registrar cooldown(int t)    { COOLDOWN.put(key(s,a), t);   return this; }
        Registrar chargeup(int t)    { CHARGEUP.put(key(s,a), t);   return this; }
        Registrar holdInterval(int t){ HOLD_RATE.put(key(s,a), t);  return this; }
    }

    private static int scaleForContainer(int baseTicks, Item cooldownItem) {
        if (baseTicks <= 0) return 0;
        if (cooldownItem instanceof InfinityGauntletItem) return Math.max(baseTicks / 3, 40); // ≥2s
        if (cooldownItem instanceof StoneHolderItem)      return Math.max(baseTicks / 2, 70); // ≥3.5s
        return baseTicks; // RAW
    }

    private static Item findContainerItemForStone(Player player, ItemStack stoneStack) {
        for (ItemStack inv : player.getInventory().items) {
            if (inv.isEmpty()) continue;
            if (inv.getItem() instanceof InfinityGauntletItem) {
                for (int i = 0; i < 6; i++) {
                    ItemStack gs = InfinityGauntletItem.getStoneStack(inv, i);
                    if (!gs.isEmpty() && ItemStack.isSameItemSameTags(gs, stoneStack)) return inv.getItem();
                }
            } else if (inv.getItem() instanceof StoneHolderItem) {
                ItemStack hs = StoneHolderItem.getStone(inv);
                if (!hs.isEmpty() && ItemStack.isSameItemSameTags(hs, stoneStack)) return inv.getItem();
            }
        }
        return null;
    }

    private static long getPlayerCooldown(Player player, String stoneKey, String abilityKey) {
        CompoundTag root = player.getPersistentData().getCompound(NBT_ROOT);
        String k = key(stoneKey, abilityKey);
        return root.contains(k) ? root.getLong(k) : 0L;
    }

    private static void setPlayerCooldown(Player player, String stoneKey, String abilityKey, long endTick) {
        CompoundTag root = player.getPersistentData().getCompound(NBT_ROOT);
        root.putLong(key(stoneKey, abilityKey), endTick);
        player.getPersistentData().put(NBT_ROOT, root);
    }
}
