package org.jcs.egm.stones.stone_reality;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client-only renderer for morph: draws a mob "puppet" in place of the player.
 * - Renders in the player's transform (no parallax), cancels vanilla player render.
 * - Disables AI on the puppet so idle goals don't animate it.
 * - Forces adult size + refreshDimensions each frame (prevents 1-block height).
 * - Locks yaw AFTER tick() to avoid turn jitter.
 * - Drives walk animation from player movement; tuned per-mob to avoid cartoon legs.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientMorphRenderHook {

    // Buckets (IDs lowercase "namespace:path")
    private static final Set<String> FLYERS = Set.of(
            "minecraft:bat","minecraft:bee","minecraft:parrot","minecraft:phantom",
            "minecraft:ghast","minecraft:blaze","minecraft:vex","minecraft:allay"
    );
    private static final Set<String> FISHLIKE = Set.of(
            "minecraft:cod","minecraft:salmon","minecraft:pufferfish","minecraft:tropical_fish",
            "minecraft:axolotl","minecraft:squid","minecraft:glow_squid","minecraft:dolphin",
            "minecraft:guardian","minecraft:elder_guardian","minecraft:turtle"
    );
    private static final Set<String> SPIDERS = Set.of("minecraft:spider","minecraft:cave_spider");
    private static final Set<String> GLIDERS = Set.of("minecraft:chicken");

    // Mobs to keep extra-still at idle (no head bob)
    private static final Set<String> QUIET_IDLE = Set.of(
            "minecraft:cow","minecraft:sniffer","minecraft:sheep","minecraft:pig","minecraft:villager"
    );

    // --- Gait tuning (how fast the walk cycle advances per block moved) ---
    private static final Map<String, Float> GAIT_SCALE = Map.of(
            "minecraft:cow",      1.8f,
            "minecraft:sniffer",  1.1f,
            "minecraft:pig",      2.2f,
            "minecraft:sheep",    2.0f,
            "minecraft:villager", 2.0f
    );
    // Upper bound for the animation speed (prevents cartoon legs)
    private static final Map<String, Float> GAIT_MAX = Map.of(
            "minecraft:cow",      0.42f,
            "minecraft:sniffer",  0.30f,
            "minecraft:pig",      0.55f,
            "minecraft:sheep",    0.50f,
            "minecraft:villager", 0.50f
    );
    // Damping (lower = lazier legs, higher = snappier)
    private static final Map<String, Float> GAIT_DAMP = Map.of(
            "minecraft:cow",      0.45f,
            "minecraft:sniffer",  0.40f,
            "minecraft:pig",      0.55f,
            "minecraft:sheep",    0.50f,
            "minecraft:villager", 0.50f
    );

    private static final class Puppet {
        final LivingEntity entity;
        long lastTicked = Long.MIN_VALUE;
        Puppet(LivingEntity e) { this.entity = e; }
    }
    private static final Map<UUID, Puppet> PUPPETS = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre evt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // keep first-person arms
        if (mc.options.getCameraType().isFirstPerson()) return;

        Player player = evt.getEntity();
        var cap = player.getCapability(MorphData.CAP);
        if (!cap.isPresent()) return;
        MorphData.MorphState state = cap.orElse(null);
        if (state == null || state.mobId == null) { clear(player.getUUID()); return; }

        LivingEntity puppet = getOrCreate(player.getUUID(), state.mobId);
        if (puppet == null) return;

        final float pt = evt.getPartialTick();

        // Interpolated player pos for internal puppet state & small tweaks
        final double px = Mth.lerp(pt, player.xOld, player.getX());
        final double py = Mth.lerp(pt, player.yOld, player.getY());
        final double pz = Mth.lerp(pt, player.zOld, player.getZ());

        // copy "old" before modifying
        puppet.xOld = puppet.getX();
        puppet.yOld = puppet.getY();
        puppet.zOld = puppet.getZ();
        puppet.yRotO = puppet.getYRot();
        puppet.yBodyRotO = puppet.yBodyRot;
        puppet.yHeadRotO = puppet.yHeadRot;

        // place puppet at player pos (renderer stack is already at player)
        puppet.setPos(px, py, pz);
        puppet.setInvisible(false);
        puppet.noPhysics = true;
        puppet.setNoGravity(true);

        puppet.setOnGround(player.onGround());
        puppet.setSwimming(player.isSwimming());
        puppet.setSprinting(player.isSprinting());
        puppet.setShiftKeyDown(player.isShiftKeyDown());
        puppet.setPose(player.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);

        // adult + correct hitbox every frame
        if (puppet instanceof AgeableMob a) { a.setBaby(false); a.setAge(0); }
        puppet.refreshDimensions();

        // advance puppet anims once per world tick
        Puppet slot = PUPPETS.get(player.getUUID());
        long gt = mc.level.getGameTime();
        if (slot != null && slot.lastTicked != gt) {
            slot.lastTicked = gt;
            puppet.tickCount = player.tickCount;
            puppet.tick();
            if (puppet.getHealth() <= 0) puppet.setHealth(puppet.getMaxHealth());
        }

        // lock yaw AFTER tick() to avoid turn jitter
        float bodyYaw = Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot);
        puppet.setYRot(bodyYaw);
        puppet.yBodyRot = bodyYaw;
        puppet.yHeadRot = bodyYaw;
        puppet.yRotO     = bodyYaw;
        puppet.yBodyRotO = bodyYaw;
        puppet.yHeadRotO = bodyYaw;

        // ---- Drive puppet walk cycle from the player's actual movement (tuned per mob) ----
        String id = state.mobId.toString();
        Vec3 pv = player.getDeltaMovement();
        boolean grounded = player.onGround() && !player.isSwimming();

        double dx = player.getX() - player.xOld;
        double dz = player.getZ() - player.zOld;
        float horiz = (float) Math.sqrt(dx * dx + dz * dz);

        float baseScale = GAIT_SCALE.getOrDefault(id, 3.6f);
        if (player.isSprinting()) baseScale *= 1.10f;
        else                      baseScale *= 0.85f;

        float limbSpeed = horiz * baseScale;
        float limit     = GAIT_MAX.getOrDefault(id, 0.60f);
        limbSpeed = Mth.clamp(limbSpeed, 0.0f, limit);
        if (!grounded) limbSpeed = 0.0f;

        float damp = GAIT_DAMP.getOrDefault(id, 0.60f);
        try {
            puppet.walkAnimation.setSpeed(limbSpeed);
            puppet.walkAnimation.update(limbSpeed, damp); // 1.20.x signature (speed, damping)
        } catch (Throwable ignored) {}

        puppet.setDeltaMovement(pv);
        if (pv.lengthSqr() < 1.0E-4) {
            puppet.setDeltaMovement(Vec3.ZERO);
            try {
                puppet.walkAnimation.setSpeed(0.0f);
                puppet.walkAnimation.update(0.0f, 0.0f);
            } catch (Throwable ignored) {}
        }

        // quiet idle head bob for certain mobs
        if (pv.lengthSqr() < 1.0E-5 && QUIET_IDLE.contains(id)) {
            puppet.yHeadRot = puppet.yBodyRot = puppet.getYRot();
            puppet.yHeadRotO = puppet.yBodyRotO = puppet.yRotO;
        }

        // per-mob polish (gentle hover, swim undulation, etc.)
        Vec3 velInterp = new Vec3(px - player.xOld, py - player.yOld, pz - player.zOld);
        applyMobTweaks(puppet, player, id, pt, velInterp);

        // render puppet in the SAME transform context as the player
        PoseStack pose = evt.getPoseStack();
        MultiBufferSource buffers = evt.getMultiBufferSource();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        pose.pushPose();
        dispatcher.render(puppet, 0.0, 1.0E-3, 0.0, puppet.getYRot(), pt, pose, buffers, evt.getPackedLight());
        pose.popPose();

        // hide vanilla player model while morphed
        evt.setCanceled(true);
    }

    // ---------- Puppet management ----------
    @Nullable
    private static LivingEntity getOrCreate(UUID id, ResourceLocation mobId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        Puppet cached = PUPPETS.get(id);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(mobId);
        if (type == null) return null;

        if (cached == null || cached.entity.getType() != type) {
            if (cached != null) cached.entity.discard();
            Entity e = type.create(mc.level);
            if (!(e instanceof LivingEntity le)) return null;

            // core puppet flags
            le.noPhysics = true;
            le.setNoGravity(true);
            le.setInvisible(false);

            // adult by default if ageable
            if (le instanceof AgeableMob a) { a.setBaby(false); a.setAge(0); }

            // disable AI so idle goals don't animate
            if (le instanceof Mob mob) {
                mob.setNoAi(true);
                try { mob.getNavigation().stop(); } catch (Throwable ignored) {}
                try { mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0); } catch (Throwable ignored) {}
            }

            le.refreshDimensions();
            cached = new Puppet(le);
            PUPPETS.put(id, cached);
        }
        return cached.entity;
    }

    public static void clear(UUID id) {
        Puppet p = PUPPETS.remove(id);
        if (p != null) p.entity.discard();
    }

    // ---------- Per-mob tweaks ----------
    private static void applyMobTweaks(LivingEntity puppet, Player src, String id, float pt, Vec3 v) {
        // Flyers: hover + pitch by vertical speed
        if (FLYERS.contains(id)) {
            puppet.setOnGround(false);
            double t = (src.tickCount + pt) * 0.10;
            puppet.setPos(puppet.getX(), puppet.getY() + Math.sin(t) * 0.06, puppet.getZ());
            puppet.setXRot((float) Mth.clamp(-v.y * 45.0, -30.0, 30.0));

            if (id.equals("minecraft:phantom")) {
                float bank = (float) Mth.clamp(v.x * 120.0, -25.0, 25.0);
                puppet.yBodyRot = puppet.getYRot() + bank * 0.05f;
                puppet.yHeadRot = puppet.yBodyRot;
            }
            if (id.equals("minecraft:ghast") || id.equals("minecraft:blaze")) {
                double slow = (src.tickCount + pt) * 0.05;
                puppet.setPos(puppet.getX(), puppet.getY() + Math.sin(slow) * 0.12, puppet.getZ());
            }
        }

        // Swimmers: align yaw/pitch to velocity, gentle undulation
        if (FISHLIKE.contains(id) || src.isInWaterOrBubble()) {
            puppet.setSwimming(true);
            if (v.lengthSqr() > 1.0E-4) {
                float swimYaw = (float)(Mth.atan2(-v.x, v.z) * (180F / Math.PI));
                puppet.setYRot(swimYaw);
                puppet.yBodyRot = swimYaw;
                puppet.yHeadRot = swimYaw;
                puppet.setXRot((float) Mth.clamp(v.y * -35.0, -35.0, 35.0));
            }
            double wiggleT = (src.tickCount + pt) * 0.33;
            double wiggle = Math.sin(wiggleT) * 0.08;
            Vec3 sideways = new Vec3(Mth.cos(puppet.getYRot()*Mth.DEG_TO_RAD),0,Mth.sin(puppet.getYRot()*Mth.DEG_TO_RAD))
                    .yRot((float)(Math.PI/2.0));
            puppet.setPos(puppet.getX()+sideways.x*wiggle, puppet.getY(), puppet.getZ()+sideways.z*wiggle);
        }

        // Chicken: glide pose when airborne
        if ("minecraft:chicken".equals(id) && !src.onGround() && !src.isInWaterOrBubble()) {
            puppet.setOnGround(false);
            puppet.setXRot((float) Mth.clamp(v.y * -25.0, -10.0, 15.0));
        }

        // Spiders: light cling hint when pressing against walls
        if (SPIDERS.contains(id)) {
            boolean walling = src.horizontalCollision && !src.onGround();
            if (walling) {
                puppet.setOnGround(true);
                puppet.setXRot(90f * 0.15f);
            } else {
                puppet.setOnGround(src.onGround());
            }
        }
    }
}
