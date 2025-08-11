package org.jcs.egm.stones.stone_time;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import org.jcs.egm.network.NetworkHandler;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;

import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class PacifyTimeStoneAbility implements IGStoneAbility {

    @Override public String abilityKey() { return "pacify"; }
    @Override public boolean canHoldUse() { return true; }

    private static final int CHARGE_TICKS = 80;
    private static final boolean AUTO_FIRE_AT_FULL = true;
    private static final double AOE_RADIUS = 8.0;

    private static final SoundEvent CHARGING_SOUND =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "time_stone_charging"));
    private static final SoundEvent TWINKLE_SOUND =
            SoundEvent.createVariableRangeEvent(new ResourceLocation("egm", "universal_twinkle"));

    private static final int COLOR_A = 0x62FF2D;
    private static final int COLOR_B = 0x0AAA67;

    private static final Set<UUID> CHARGING_SOUND_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> CHARGE = new HashMap<>();

    @Override public void activate(Level level, Player player, ItemStack stack) {}

    @Override
    public void onUsingTick(Level level, Player player, ItemStack stack, int count) {
        final UUID id = player.getUUID();
        final int useDuration = player.getUseItem().getUseDuration();
        final int ticksHeld = useDuration - count;

        if (ticksHeld < CHARGE_TICKS) {
            if (!level.isClientSide) {
                CHARGE.put(id, ticksHeld);
                if ((player.tickCount & 1) == 0) {
                    NetworkHandler.sendWristRing(player, ticksHeld, COLOR_A, COLOR_B);
                }
            } else if (!CHARGING_SOUND_PLAYERS.contains(id)) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        CHARGING_SOUND, SoundSource.PLAYERS, 0.9f, 1.0f, true);
                CHARGING_SOUND_PLAYERS.add(id);
            }
            return;
        }

        if (!level.isClientSide && (player.tickCount & 1) == 0) {
            NetworkHandler.sendWristRing(player, ticksHeld, COLOR_A, COLOR_B);
        }

        if (AUTO_FIRE_AT_FULL && !level.isClientSide) {
            Integer prev = CHARGE.get(id);
            if (prev == null || prev < CHARGE_TICKS) {
                CHARGE.put(id, CHARGE_TICKS);
                doPacify(level, player, stack);
                if (player instanceof ServerPlayer sp) sp.stopUsingItem();
            }
        } else if (AUTO_FIRE_AT_FULL && level.isClientSide) {
            stopChargingSoundClient(id);
        }
    }

    @Override
    public void releaseUsing(Level level, Player player, ItemStack stack, int count) {
        final UUID id = player.getUUID();
        Integer charged = CHARGE.remove(id);
        if (level.isClientSide) stopChargingSoundClient(id);
        if (AUTO_FIRE_AT_FULL) return;
        if (charged == null || charged < CHARGE_TICKS) return;

        if (!level.isClientSide) {
            doPacify(level, player, stack);
        }
    }

    private void doPacify(Level level, Player player, ItemStack stoneStack) {
        if (StoneAbilityCooldowns.guardUse(player, stoneStack, "time", this)) return;

        boolean changedAny = false;

        // Grab mobs in radius
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(AOE_RADIUS));
        for (Mob mob : mobs) {
            if (mob instanceof AgeableMob ageable && !ageable.isBaby()) {
                // Pacify: make baby for entities that have baby variants
                ageable.setBaby(true);
                changedAny = true;

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ModParticles.TIME_STONE_EFFECT_ONE.get(),
                            mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                            12, 0.3, 0.3, 0.3, 0.01);
                }
            } else {
                // NEW: Entities that don't have babies → convert to their spawn egg (and remove mob)
                if (!(level instanceof ServerLevel sl)) continue;

                ItemStack egg = findSpawnEgg(mob.getType());
                if (egg.isEmpty()) continue; // skip if no egg exists

                BlockPos pos = mob.blockPosition();
                mob.remove(RemovalReason.DISCARDED);

                ItemEntity dropped = new ItemEntity(
                        sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, egg
                );
                dropped.setDeltaMovement(0.0, 0.01, 0.0);
                // mark as newly created by pacify (no follow-up behavior)
                CompoundTag tag = dropped.getPersistentData();
                tag.putBoolean("PacifyNewEgg", true);
                sl.addFreshEntity(dropped);

                sl.sendParticles(ModParticles.TIME_STONE_EFFECT_ONE.get(),
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        24, 0.2, 0.3, 0.2, 0.03);

                changedAny = true;
            }
        }

        if (changedAny) {
            level.playSound(null, player.blockPosition(), TWINKLE_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
            StoneAbilityCooldowns.apply(player, stoneStack, "time", this);
        } else if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("Nothing to pacify."), true);
        }
    }

    private ItemStack findSpawnEgg(EntityType<?> type) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (key == null) return ItemStack.EMPTY;
        // 1) direct <modid>:<entity>_spawn_egg
        var rl = new ResourceLocation(key.getNamespace(), key.getPath() + "_spawn_egg");
        if (ForgeRegistries.ITEMS.containsKey(rl)) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
        // 2) fallback: scan registered eggs for matching type
        for (Item itm : ForgeRegistries.ITEMS) {
            if (itm instanceof SpawnEggItem se && se.getType(null) == type) {
                return new ItemStack(itm);
            }
        }
        return ItemStack.EMPTY;
    }

    private void stopChargingSoundClient(UUID id) {
        if (!CHARGING_SOUND_PLAYERS.contains(id)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().stop(CHARGING_SOUND.getLocation(), SoundSource.PLAYERS);
        }
        CHARGING_SOUND_PLAYERS.remove(id);
    }
}
