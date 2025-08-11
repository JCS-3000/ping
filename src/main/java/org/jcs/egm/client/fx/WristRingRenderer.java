package org.jcs.egm.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "egm", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WristRingRenderer {

    // ===== Tuning =====
    private static final ResourceLocation TEX = new ResourceLocation("egm", "textures/misc/wrist_panel.png");
    private static final int SEGMENTS = 8;           // panels around the circle (octagon)
    private static final float RADIUS = 0.24f;       // meters around the wrist
    private static final float HALF_WIDTH = 0.06f;   // along ring tangent
    private static final float HALF_HEIGHT = 0.13f;  // along forearm axis
    private static final float SPIN_SPEED = 0.30f;   // rad/tick
    private static final int FULL_BRIGHT = 0xF000F0; // packed light

    // Active rings keyed by player id; refreshed by packet, expires fast if you stop charging
    private static final Map<Integer, Entry> ACTIVE = new HashMap<>();
    private record Entry(int ticksHeld, int colorA, int colorB, int ttl) {}

    /** Called from your S2C packet handler (client thread). */
    public static void push(int entityId, int ticksHeld, int colorAHex, int colorBHex) {
        ACTIVE.put(entityId, new Entry(ticksHeld, colorAHex, colorBHex, 4)); // ~2 ticks margin
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (ACTIVE.isEmpty() || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        final Vec3 camPos = e.getCamera().getPosition();
        final MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        final PoseStack ps = e.getPoseStack();

        ps.pushPose();
        // world → camera space
        ps.translate(-camPos.x, -camPos.y, -camPos.z);

        ACTIVE.entrySet().removeIf(it -> {
            Player p = (Player) mc.level.getEntity(it.getKey());
            Entry entry = it.getValue();
            if (p == null) return true;

            drawRing(ps, buffers, p, entry, e.getPartialTick());

            int nextTtl = entry.ttl - 1;
            if (nextTtl <= 0) return true;
            it.setValue(new Entry(entry.ticksHeld, entry.colorA, entry.colorB, nextTtl));
            return false;
        });

        ps.popPose();
        // Do NOT endBatch() here; Minecraft/Forge manages the global buffer lifecycle.
    }

    private static void drawRing(PoseStack ps, MultiBufferSource buffers, Player player, Entry entry, float pt) {
        // Anchor near the main hand: slightly forward and to the side of the chest
        Vec3 base = new Vec3(
                Mth.lerp(pt, player.xo, player.getX()),
                Mth.lerp(pt, player.yo, player.getY()) + player.getEyeHeight() * 0.6,
                Mth.lerp(pt, player.zo, player.getZ())
        );

        Vec3 forward = player.getViewVector(pt).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        if (player.getMainArm() == HumanoidArm.LEFT) right = right.scale(-1);
        Vec3 center = base.add(forward.scale(0.25)).add(right.scale(0.25));

        // Build an orthonormal basis for ring plane (u,v) perpendicular to forearm axis (forward)
        Vec3 u = right;                    // lateral
        Vec3 v = forward.cross(u).normalize(); // vertical around the wrist

        float spin = (player.tickCount + pt) * SPIN_SPEED;
        float intensity = Mth.clamp(entry.ticksHeld / 80.0f, 0f, 1f);
        float alpha = 0.20f + 0.65f * intensity; // gentle fade-in while charging

        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));

        for (int i = 0; i < SEGMENTS; i++) {
            float t = (float) (i * (Math.PI * 2.0 / SEGMENTS) + spin);

            // radial & tangent directions on the ring plane
            Vec3 radial = u.scale(Mth.cos(t)).add(v.scale(Mth.sin(t))).normalize();
            Vec3 tangent = u.scale(-Mth.sin(t)).add(v.scale(Mth.cos(t))).normalize();

            // panel half-axes
            Vec3 ax = tangent.scale(HALF_WIDTH * 2f);
            Vec3 ay = forward.scale(HALF_HEIGHT * 2f);

            // center of this panel on the ring
            Vec3 c = center.add(radial.scale(RADIUS));

            // quad corners (world space)
            Vec3 p0 = c.subtract(ax).subtract(ay);
            Vec3 p1 = c.add(ax).subtract(ay);
            Vec3 p2 = c.add(ax).add(ay);
            Vec3 p3 = c.subtract(ax).add(ay);

            // alternate color by segment
            int hex = (i & 1) == 0 ? entry.colorA : entry.colorB;
            float r = ((hex >> 16) & 0xFF) / 255f;
            float g = ((hex >> 8) & 0xFF) / 255f;
            float b = (hex & 0xFF) / 255f;

            // outward normal for lighting (even though we're fullbright, the format expects a normal)
            Vec3 n = p1.subtract(p0).cross(p2.subtract(p0)).normalize();
            float nx = (float) n.x;
            float ny = (float) n.y;
            float nz = (float) n.z;

            // front face
            putQuad(ps, vc, p0, p1, p2, p3, r, g, b, alpha, FULL_BRIGHT, nx, ny, nz);
            // back face (reverse winding, invert normal)
            putQuad(ps, vc, p3, p2, p1, p0, r, g, b, alpha, FULL_BRIGHT, -nx, -ny, -nz);
        }
    }

    private static void putQuad(
            PoseStack ps, VertexConsumer vc,
            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
            float r, float g, float b, float a,
            int light, float nx, float ny, float nz
    ) {
        var pose = ps.last();
        // Two triangles, full UVs, with overlay + light + normal (REQUIRED for this render type)
        vc.vertex(pose.pose(), (float) p0.x, (float) p0.y, (float) p0.z)
                .color(r, g, b, a)
                .uv(0f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();

        vc.vertex(pose.pose(), (float) p1.x, (float) p1.y, (float) p1.z)
                .color(r, g, b, a)
                .uv(1f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();

        vc.vertex(pose.pose(), (float) p2.x, (float) p2.y, (float) p2.z)
                .color(r, g, b, a)
                .uv(1f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();

        vc.vertex(pose.pose(), (float) p3.x, (float) p3.y, (float) p3.z)
                .color(r, g, b, a)
                .uv(0f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }
}
