package org.jcs.egm.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustColorTransitionOptions; // FIX: use options class
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f; // needed by DustColorTransitionOptions

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WristRingRenderer {

    private static final ResourceLocation TEX_WHITE = new ResourceLocation("egm", "textures/misc/white.png");

    // ===== Placement near the wrist =====
    private static final float SIDE_OFFSET     = 0.28f; // toward main hand from chest
    private static final float FORWARD_OFFSET  = 0.42f; // toward hand from chest
    private static final float WRIST_Y_ADJ     = -0.35f; // eyeHeight + this ≈ wrist

    // ===== 3D band params =====
    private static final int   SEGMENTS           = 16;    // smoothness around the ring
    private static final float RADIUS             = 0.18f; // ring radius around wrist center
    private static final float HALF_RADIAL        = 0.020f; // thickness outward/inward
    private static final float HALF_FOREARM       = 0.020f; // band height along the arm
    private static final float SPIN_SPEED         = 0.30f;  // rad/tick spin
    private static final int   FULL_BRIGHT        = 0xF000F0;

    // Optional second ring (clone) toward the fist; set RING_COUNT=1 for single band.
    private static final int   RING_COUNT         = 2;     // 1 = single ring, 2 = add second
    private static final float RING_FORWARD_STEP  = 0.15f; // distance between rings along arm

    // === Particle tuning (small, sparse drift) ===
    private static final float PARTICLE_SCALE        = 0.55f;  // dust size (0.0–4.0 ish)
    private static final float PARTICLE_SPEED_BASE   = 0.015f; // outward push
    private static final float PARTICLE_SPEED_JITTER = 0.010f; // random extra
    private static final float PARTICLE_CHANCE       = 0.06f;  // per segment per ring per frame
    private static final int   PARTICLE_CAP_PER_TICK = 6;      // safety cap per ring per frame

    // Active rings keyed by player id; refreshed by packet, quick TTL so it fades if updates stop
    private static final Map<Integer, Entry> ACTIVE = new HashMap<>();
    private record Entry(int ticksHeld, int colorA, int colorB, int ttl) {}

    /** Called from S2C packet handler (client thread). */
    public static void push(int entityId, int ticksHeld, int colorAHex, int colorBHex) {
        ACTIVE.put(entityId, new Entry(ticksHeld, colorAHex, colorBHex, 4));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (ACTIVE.isEmpty() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        final Vec3 cam = e.getCamera().getPosition();
        final MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        final PoseStack ps = e.getPoseStack();

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        ACTIVE.entrySet().removeIf(it -> {
            Player p = (Player) mc.level.getEntity(it.getKey());
            Entry en = it.getValue();
            if (p == null) return true;

            drawRing(ps, buffers, p, en, e.getPartialTick());

            int ttl = en.ttl - 1;
            if (ttl <= 0) return true;
            it.setValue(new Entry(en.ticksHeld, en.colorA, en.colorB, ttl));
            return false;
        });

        ps.popPose();
        // Do not endBatch(); Forge handles global buffers.
    }

    private static void drawRing(PoseStack ps, MultiBufferSource bufs, Player pl, Entry en, float pt) {
        // Wrist anchor
        double x = Mth.lerp(pt, pl.xo, pl.getX());
        double y = pl.getY() + pl.getEyeHeight() + WRIST_Y_ADJ;
        double z = Mth.lerp(pt, pl.zo, pl.getZ());
        Vec3 base = new Vec3(x, y, z);

        // Local axes: fwd ~ forearm axis; right ~ lateral; up = fwd × right
        Vec3 fwd = pl.getViewVector(pt);
        if (Math.abs(fwd.y) > 0.98) fwd = new Vec3(fwd.x, Mth.clamp(fwd.y, -0.98, 0.98), fwd.z);
        fwd = fwd.normalize();
        Vec3 right = fwd.cross(new Vec3(0, 1, 0)).normalize();
        if (pl.getMainArm() == HumanoidArm.LEFT) right = right.scale(-1);
        Vec3 up = fwd.cross(right).normalize();

        Vec3 center = base.add(fwd.scale(FORWARD_OFFSET)).add(right.scale(SIDE_OFFSET));
        float spin = (pl.tickCount + pt) * SPIN_SPEED;

        VertexConsumer vc = bufs.getBuffer(RenderType.entityTranslucentEmissive(TEX_WHITE));
        final float alphaOuter = 0.70f; // slightly transparent
        final float alphaInner = 0.60f;
        final float alphaCaps  = 0.65f;

        // Optional second ring closer to the fist
        for (int ringIdx = 0; ringIdx < RING_COUNT; ringIdx++) {
            Vec3 ringCenter = center.add(fwd.scale(ringIdx * RING_FORWARD_STEP));

            // per-ring particle budget this frame
            int emitted = 0;

            for (int i = 0; i < SEGMENTS; i++) {
                float t0 = (float) (i * (Math.PI * 2.0 / SEGMENTS) + spin);
                float t1 = (float) ((i + 1) * (Math.PI * 2.0 / SEGMENTS) + spin);

                // Radial directions at segment edges
                Vec3 radial0 = right.scale(Mth.cos(t0)).add(up.scale(Mth.sin(t0))).normalize();
                Vec3 radial1 = right.scale(Mth.cos(t1)).add(up.scale(Mth.sin(t1))).normalize();
                Vec3 radialM = radial0.add(radial1).normalize(); // for smoother side normals

                float Rin = RADIUS - HALF_RADIAL;
                float Rout = RADIUS + HALF_RADIAL;
                float yNeg = -HALF_FOREARM, yPos = HALF_FOREARM;

                // 8 verts for this segment (inner/outer × bottom/top)
                Vec3 i0b = ringCenter.add(radial0.scale(Rin)).add(fwd.scale(yNeg));
                Vec3 i1b = ringCenter.add(radial1.scale(Rin)).add(fwd.scale(yNeg));
                Vec3 i1t = ringCenter.add(radial1.scale(Rin)).add(fwd.scale(yPos));
                Vec3 i0t = ringCenter.add(radial0.scale(Rin)).add(fwd.scale(yPos));

                Vec3 o0b = ringCenter.add(radial0.scale(Rout)).add(fwd.scale(yNeg));
                Vec3 o1b = ringCenter.add(radial1.scale(Rout)).add(fwd.scale(yNeg));
                Vec3 o1t = ringCenter.add(radial1.scale(Rout)).add(fwd.scale(yPos));
                Vec3 o0t = ringCenter.add(radial0.scale(Rout)).add(fwd.scale(yPos));

                // color per segment (alternating)
                int hex = (i & 1) == 0 ? en.colorA : en.colorB;
                float cr = ((hex >> 16) & 0xFF) / 255f;
                float cg = ((hex >>  8) & 0xFF) / 255f;
                float cb = ( hex        & 0xFF) / 255f;

                // ========== SURFACES (double-sided) ==========
                // OUTER cylinder (normal ≈ +radial)
                putQuad(ps, vc, o0b, o1b, o1t, o0t, cr, cg, cb, alphaOuter, FULL_BRIGHT,
                        (float) radialM.x, (float) radialM.y, (float) radialM.z);
                putQuad(ps, vc, o0t, o1t, o1b, o0b, cr, cg, cb, alphaOuter, FULL_BRIGHT,
                        -(float) radialM.x, -(float) radialM.y, -(float) radialM.z);

                // INNER cylinder (normal ≈ -radial)
                putQuad(ps, vc, i0t, i1t, i1b, i0b, cr, cg, cb, alphaInner, FULL_BRIGHT,
                        -(float) radialM.x, -(float) radialM.y, -(float) radialM.z);
                putQuad(ps, vc, i0b, i1b, i1t, i0t, cr, cg, cb, alphaInner, FULL_BRIGHT,
                        (float) radialM.x,  (float) radialM.y,  (float) radialM.z);

                // TOP cap (toward fist, normal ≈ +fwd)
                putQuad(ps, vc, i0t, i1t, o1t, o0t, cr, cg, cb, alphaCaps, FULL_BRIGHT,
                        (float) fwd.x, (float) fwd.y, (float) fwd.z);
                putQuad(ps, vc, o0t, o1t, i1t, i0t, cr, cg, cb, alphaCaps, FULL_BRIGHT,
                        -(float) fwd.x, -(float) fwd.y, -(float) fwd.z);

                // BOTTOM cap (toward elbow, normal ≈ -fwd)
                putQuad(ps, vc, o0b, o1b, i1b, i0b, cr, cg, cb, alphaCaps, FULL_BRIGHT,
                        -(float) fwd.x, -(float) fwd.y, -(float) fwd.z);
                putQuad(ps, vc, i0b, i1b, o1b, o0b, cr, cg, cb, alphaCaps, FULL_BRIGHT,
                        (float) fwd.x,  (float) fwd.y,  (float) fwd.z);

                // === LIGHT PARTICLE EMISSION (FIXED) ===
                if (emitted < PARTICLE_CAP_PER_TICK && ThreadLocalRandom.current().nextFloat() < PARTICLE_CHANCE) {
                    // point along outer top edge (slight lift to avoid z-fighting)
                    float lerp = ThreadLocalRandom.current().nextFloat();
                    Vec3 edgePt = new Vec3(
                            Mth.lerp(lerp, o0t.x, o1t.x),
                            Mth.lerp(lerp, o0t.y, o1t.y),
                            Mth.lerp(lerp, o0t.z, o1t.z)
                    ).add(radialM.scale(0.002));

                    // outward velocity with a touch of fwd/up jitter
                    double speed = PARTICLE_SPEED_BASE + ThreadLocalRandom.current().nextDouble() * PARTICLE_SPEED_JITTER;
                    Vec3 vel = radialM.normalize().scale(speed)
                            .add(fwd.scale(0.004 * (ThreadLocalRandom.current().nextDouble() - 0.5)))
                            .add(up.scale(0.004 * (ThreadLocalRandom.current().nextDouble() - 0.5)));

                    // transition: segment color -> slightly brighter
                    float br = Mth.clamp(cr * 1.15f, 0f, 1f);
                    float bg = Mth.clamp(cg * 1.15f, 0f, 1f);
                    float bb = Mth.clamp(cb * 1.15f, 0f, 1f);

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        DustColorTransitionOptions opts = new DustColorTransitionOptions(
                                new Vector3f(cr, cg, cb),
                                new Vector3f(br, bg, bb),
                                PARTICLE_SCALE
                        );
                        // FIX: pass options, not the particle TYPE
                        mc.level.addParticle(
                                opts,
                                edgePt.x, edgePt.y, edgePt.z,
                                vel.x,   vel.y,   vel.z
                        );
                    }
                    emitted++;
                }
            }
        }
    }

    private static void putQuad(
            PoseStack ps, VertexConsumer vc,
            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
            float r, float g, float b, float a,
            int light, float nx, float ny, float nz
    ) {
        var pose = ps.last();

        vc.vertex(pose.pose(), (float) p0.x, (float) p0.y, (float) p0.z)
                .color(r, g, b, a).uv(0f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();

        vc.vertex(pose.pose(), (float) p1.x, (float) p1.y, (float) p1.z)
                .color(r, g, b, a).uv(1f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();

        vc.vertex(pose.pose(), (float) p2.x, (float) p2.y, (float) p2.z)
                .color(r, g, b, a).uv(1f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();

        vc.vertex(pose.pose(), (float) p3.x, (float) p3.y, (float) p3.z)
                .color(r, g, b, a).uv(0f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
