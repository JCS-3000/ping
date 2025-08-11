package org.jcs.egm.stones.stone_reality;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.event.entity.player.PlayerEvent; // NEW: logout hook
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jcs.egm.config.ModCommonConfig;

import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MorphData {
    public static final ResourceLocation CAP_ID = new ResourceLocation("egm", "morph");
    public static final Capability<MorphState> CAP = CapabilityManager.get(new CapabilityToken<>(){});

    private static final Set<String> DEFAULT_FLY = Set.of(
            "minecraft:phantom","minecraft:ghast","minecraft:blaze","minecraft:vex","minecraft:parrot","minecraft:allay"
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
    private static final int EFFECT_LONG = 20 * 60; // 60s cushion

    private static final AttributeModifier FLY_SPEED_BOOST =
            new AttributeModifier(UUID.fromString("b0e6e4b6-1a2f-4b1e-8f7b-0a7a9b2da001"),
                    "EGM Morph Fly Boost", 0.02D, AttributeModifier.Operation.ADDITION);

    /** Back-compat with existing call site. */
    public static class Flags { public static byte none() { return 0; } }

    /** MOD-bus: register capability type. */
    @Mod.EventBusSubscriber(modid = "egm", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusHooks {
        @SubscribeEvent
        public static void onRegisterCaps(net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent evt) {
            evt.register(MorphState.class);
        }
    }

    /** Provider attached to players. */
    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final MorphState data = new MorphState();
        private final LazyOptional<MorphState> lazy = LazyOptional.of(() -> data);
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, net.minecraft.core.Direction side) {
            return cap == CAP ? lazy.cast() : LazyOptional.empty();
        }
        @Override public CompoundTag serializeNBT() { return data.serializeNBT(); }
        @Override public void deserializeNBT(CompoundTag nbt) { data.deserializeNBT(nbt); }
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> evt) {
        if (evt.getObject() instanceof Player) evt.addCapability(CAP_ID, new Provider());
    }

    /** Server-side entry point: begin/refresh a morph. */
    public static void startMorph(ServerPlayer sp, ResourceLocation mobId, int durationTicks, byte flags) {
        LazyOptional<MorphState> cap = sp.getCapability(CAP);
        if (cap.isPresent()) {
            cap.ifPresent(state -> {
                state.mobId = mobId;
                state.expiresAt = sp.level().getGameTime() + durationTicks;
                state.flags = flags;
                S2CSyncMorph.send(sp, state);     // sync client
                applyServerBuffs(sp, state);      // apply powers
            }
            );
        }
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

        // Re-assert flight while morphed as a flyer (covers GM changes/other mods)
        p.getCapability(CAP).ifPresent(state -> {
            if (!(p instanceof ServerPlayer sp) || state.mobId == null) return;
            String id = state.mobId.toString();
            Set<String> fly = merged(DEFAULT_FLY, ModCommonConfig.REALITY_METAMORPHOSIS_FLY_ENTITIES.get());
            boolean shouldFly = fly.contains(id) || id.equals("minecraft:bat") || id.endsWith(":bat") || id.endsWith(":vampire_bat");
            if (shouldFly) enableFlight(sp);
        });

        // Spider climb helper
        if (p.getPersistentData().getBoolean(SPIDER_FLAG)) {
            if (p.horizontalCollision && p.getDeltaMovement().y < 0.2) {
                p.setDeltaMovement(p.getDeltaMovement().x, 0.2, p.getDeltaMovement().z);
                p.fallDistance = 0.0F;
            }
        }
    }

    // NEW: clear flight when the player logs off (don’t end the morph; just sanitize abilities)
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent evt) {
        if (!(evt.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return; // keep creative/spectator flight
        sp.getAbilities().mayfly = false;
        sp.getAbilities().flying = false;
        sp.onUpdateAbilities();
        var inst = sp.getAttribute(Attributes.FLYING_SPEED);
        if (inst != null && inst.getModifier(FLY_SPEED_BOOST.getId()) != null) {
            inst.removeModifier(FLY_SPEED_BOOST);
        }
    }

    // ================= Powers per category =================

    private static void applyServerBuffs(ServerPlayer sp, MorphState state) {
        if (state.mobId == null) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(state.mobId);
        if (type == null) return;

        String id = state.mobId.toString();

        Set<String> fly   = merged(DEFAULT_FLY,   ModCommonConfig.REALITY_METAMORPHOSIS_FLY_ENTITIES.get());
        Set<String> swim  = merged(DEFAULT_SWIM,  ModCommonConfig.REALITY_METAMORPHOSIS_SWIM_ENTITIES.get());
        Set<String> fire  = merged(DEFAULT_FIREPROOF, ModCommonConfig.REALITY_METAMORPHOSIS_FIREPROOF_ENTITIES.get());
        Set<String> slow  = merged(DEFAULT_SLOWFALL,  ModCommonConfig.REALITY_METAMORPHOSIS_SLOWFALL_ENTITIES.get());
        Set<String> climb = merged(DEFAULT_SPIDER, ModCommonConfig.REALITY_METAMORPHOSIS_SPIDERCLIMB_ENTITIES.get());

        // Flying → mayfly + slight flying speed boost
        boolean shouldFly = fly.contains(id) || id.equals("minecraft:bat") || id.endsWith(":bat") || id.endsWith(":vampire_bat");
        if (shouldFly) {
            enableFlight(sp);
            if (id.equals("minecraft:bat") || id.endsWith(":bat")) {
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_LONG, 0, true, false));
            }
        }

        // Swimming → water breathing + dolphins grace
        if (swim.contains(id) || isWaterCategory(type)) {
            sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_LONG, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_LONG, 0, true, false));
        }

        // Fireproof
        if (fire.contains(id)) {
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
        if (!sp.isCreative() && !sp.isSpectator()) {
            sp.getAbilities().mayfly = false;
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
        }
        sp.removeEffect(MobEffects.NIGHT_VISION);
        sp.removeEffect(MobEffects.DOLPHINS_GRACE);
        sp.removeEffect(MobEffects.WATER_BREATHING);
        sp.removeEffect(MobEffects.FIRE_RESISTANCE);
        sp.removeEffect(MobEffects.SLOW_FALLING);

        var inst = sp.getAttribute(Attributes.FLYING_SPEED);
        if (inst != null && inst.getModifier(FLY_SPEED_BOOST.getId()) != null) {
            inst.removeModifier(FLY_SPEED_BOOST);
        }

        sp.getPersistentData().remove(SPIDER_FLAG);
    }

    private static Set<String> merged(Set<String> defaults, java.util.List<? extends String> cfg) {
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

    /** Ensure immediate hover + proper client sync. */
    private static void enableFlight(ServerPlayer sp) {
        if (!sp.getAbilities().mayfly || !sp.getAbilities().flying) {
            sp.getAbilities().mayfly = true;
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
        }
        var inst = sp.getAttribute(Attributes.FLYING_SPEED);
        if (inst != null && inst.getModifier(FLY_SPEED_BOOST.getId()) == null) {
            inst.addTransientModifier(FLY_SPEED_BOOST);
        }
    }

    // ===== state =====
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
            tag.putLong("ExpiresAt", expiresAt);
            tag.putByte("Flags", flags);
            return tag;
        }

        public void deserializeNBT(CompoundTag tag) {
            mobId = tag.contains("MobId") ? new ResourceLocation(tag.getString("MobId")) : null;
            expiresAt = tag.getLong("ExpiresAt");
            flags = tag.getByte("Flags");
        }
    }
}
