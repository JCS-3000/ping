package org.jcs.egm.stones.stone_reality;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jcs.egm.config.ModCommonConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class MorphData {

    public static final ResourceLocation CAP_ID = new ResourceLocation("egm", "morph");
    public static final Capability<MorphState> CAP = CapabilityManager.get(new CapabilityToken<>(){});

    // Defaults (merged with config)
    private static final Set<String> DEFAULT_FLY = Set.of(
            "minecraft:bat","minecraft:bee","minecraft:parrot","minecraft:phantom",
            "minecraft:ghast","minecraft:blaze","minecraft:vex","minecraft:allay",
            "minecraft:wither","minecraft:ender_dragon"
    );
    private static final Set<String> DEFAULT_SWIM = Set.of(
            "minecraft:squid","minecraft:glow_squid","minecraft:axolotl","minecraft:dolphin",
            "minecraft:cod","minecraft:salmon","minecraft:pufferfish","minecraft:tropical_fish",
            "minecraft:turtle","minecraft:guardian","minecraft:elder_guardian"
    );
    private static final Set<String> DEFAULT_FIREPROOF = Set.of(
            "minecraft:blaze","minecraft:wither","minecraft:ghast","minecraft:magma_cube","minecraft:strider"
    );
    private static final Set<String> DEFAULT_SLOWFALL = Set.of("minecraft:chicken");
    private static final Set<String> DEFAULT_SPIDER = Set.of("minecraft:spider","minecraft:cave_spider");

    private static final String SPIDER_FLAG = "EGM_SPIDER_CLIMB";
    private static final int EFFECT_LONG = 20 * 60; // 60s cushion; removed on morph end

    private static final AttributeModifier FLY_SPEED_BOOST =
            new AttributeModifier(UUID.fromString("b0e6e4b6-1a2f-4b1e-8f7b-0a7a9b2da001"),
                    "egm_morph_fly_speed", 0.05, AttributeModifier.Operation.ADDITION);

    public static class Flags { public static byte none() { return 0; } }

    public static class MorphState {
        @Nullable public ResourceLocation mobId;
        public long expiresAt = 0L;
        public byte flags = 0;

        public boolean isActive(Player p) {
            return mobId != null && p.level().getGameTime() < expiresAt;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            if (mobId != null) tag.putString("MobId", mobId.toString());
            tag.putLong("Expires", expiresAt);
            tag.putByte("Flags", flags);
            return tag;
        }
        public void deserializeNBT(CompoundTag tag) {
            mobId = tag.contains("MobId") ? new ResourceLocation(tag.getString("MobId")) : null;
            expiresAt = tag.getLong("Expires");
            flags = tag.getByte("Flags");
        }
    }

    public static class Provider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private final MorphState data = new MorphState();
        private final LazyOptional<MorphState> opt = LazyOptional.of(() -> data);

        @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            return cap == CAP ? opt.cast() : LazyOptional.empty();
        }
        @Override public CompoundTag serializeNBT() { return data.serializeNBT(); }
        @Override public void deserializeNBT(CompoundTag nbt) { data.deserializeNBT(nbt); }
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> evt) {
        if (evt.getObject() instanceof Player) {
            evt.addCapability(CAP_ID, new Provider());
        }
    }

    /** Server-side entry point: begin/refresh a morph. */
    public static void startMorph(ServerPlayer sp, ResourceLocation mobId, int durationTicks, byte flags) {
        sp.getCapability(CAP).ifPresent(state -> {
            state.mobId = mobId;
            state.expiresAt = sp.level().getGameTime() + durationTicks;
            state.flags = flags;
            S2CSyncMorph.send(sp, state);
            applyServerBuffs(sp, state);
        });
    }

    public static void stopMorph(ServerPlayer sp) {
        sp.getCapability(CAP).ifPresent(state -> {
            state.mobId = null;
            state.expiresAt = 0;
            state.flags = 0;
            S2CSyncMorph.send(sp, state);
            removeServerBuffs(sp);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.player.level().isClientSide) return;
        var p = evt.player;

        // Expire morph
        p.getCapability(CAP).ifPresent(state -> {
            if (state.mobId != null && p.level().getGameTime() >= state.expiresAt) {
                if (p instanceof ServerPlayer sp) stopMorph(sp);
            }
        });

        // Spider climb helper
        if (p.getPersistentData().getBoolean(SPIDER_FLAG)) {
            if (p.horizontalCollision && p.getDeltaMovement().y < 0.2) {
                p.setDeltaMovement(p.getDeltaMovement().x, 0.2, p.getDeltaMovement().z);
                p.fallDistance = 0.0F;
            }
        }
    }

    // ================= Powers per category =================

    private static void applyServerBuffs(ServerPlayer sp, MorphState state) {
        if (state.mobId == null) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(state.mobId);
        if (type == null) return;

        String id = state.mobId.toString();

        // Merge defaults + config for each bucket
        Set<String> fly   = merged(DEFAULT_FLY,   ModCommonConfig.REALITY_METAMORPHOSIS_FLY_ENTITIES.get());
        Set<String> swim  = merged(DEFAULT_SWIM,  ModCommonConfig.REALITY_METAMORPHOSIS_SWIM_ENTITIES.get());
        Set<String> fire  = merged(DEFAULT_FIREPROOF, ModCommonConfig.REALITY_METAMORPHOSIS_FIREPROOF_ENTITIES.get());
        Set<String> slow  = merged(DEFAULT_SLOWFALL,  ModCommonConfig.REALITY_METAMORPHOSIS_SLOWFALL_ENTITIES.get());
        Set<String> climb = merged(DEFAULT_SPIDER, ModCommonConfig.REALITY_METAMORPHOSIS_SPIDERCLIMB_ENTITIES.get());

        // Flying → mayfly + slight flying speed boost
        if (fly.contains(id)) {
            enableFlight(sp);
            if ("minecraft:bat".equals(id)) {
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_LONG, 0, true, false));
            }
        }

        // Swimming → water breathing + dolphins grace
        if (swim.contains(id) || isWaterCategory(type)) {
            sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_LONG, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_LONG, 0, true, false));
        }

        // Fireproof → fire resistance
        if (fire.contains(id) || type.fireImmune()) {
            sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_LONG, 0, true, false));
        }

        // Slow falling
        if (slow.contains(id)) {
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EFFECT_LONG, 0, true, false));
        }

        // Spider climb
        if (climb.contains(id)) {
            sp.getPersistentData().putBoolean(SPIDER_FLAG, true);
        }
    }

    private static void removeServerBuffs(ServerPlayer sp) {
        // Flight (preserve creative/spectator)
        if (!sp.isCreative() && !sp.isSpectator()) {
            sp.getAbilities().mayfly = false;
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
        }
        // Effects
        sp.removeEffect(MobEffects.NIGHT_VISION);
        sp.removeEffect(MobEffects.DOLPHINS_GRACE);
        sp.removeEffect(MobEffects.WATER_BREATHING);
        sp.removeEffect(MobEffects.FIRE_RESISTANCE);
        sp.removeEffect(MobEffects.SLOW_FALLING);

        // Flying speed boost — use getModifier/removeModifier(AttributeModifier)
        var inst = sp.getAttribute(Attributes.FLYING_SPEED);
        if (inst != null && inst.getModifier(FLY_SPEED_BOOST.getId()) != null) {
            inst.removeModifier(FLY_SPEED_BOOST);
        }

        sp.getPersistentData().remove(SPIDER_FLAG);
    }

    // ===== helpers =====
    private static Set<String> merged(Set<String> defaults, List<? extends String> cfg) {
        Set<String> out = new HashSet<>(defaults);
        if (cfg != null) out.addAll(cfg);
        return out;
    }
    private static boolean isWaterCategory(EntityType<?> t) {
        MobCategory c = t.getCategory();
        return c == MobCategory.WATER_CREATURE
                || c == MobCategory.WATER_AMBIENT
                || c == MobCategory.UNDERGROUND_WATER_CREATURE
                || c == MobCategory.AXOLOTLS;
    }
    private static void enableFlight(ServerPlayer sp) {
        sp.getAbilities().mayfly = true;
        sp.onUpdateAbilities();
        var inst = sp.getAttribute(Attributes.FLYING_SPEED);
        if (inst != null && inst.getModifier(FLY_SPEED_BOOST.getId()) == null) {
            inst.addTransientModifier(FLY_SPEED_BOOST);
        }
    }
}
