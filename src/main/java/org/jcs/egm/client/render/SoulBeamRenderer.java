package org.jcs.egm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.entity.SoulBeamEntity;
import org.joml.Quaternionf;

public class SoulBeamRenderer extends EntityRenderer<SoulBeamEntity> {
    private static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation("egm:textures/entity/placeholder.png");

    private static final int SEGMENTS = 5;
    private static final float SPREAD = 0.3f; // Slightly less chaotic than lightning

    // Soul beam widths (slightly thinner than power stone lightning)
    private static final float[] BOX_WIDTHS = {
            0.40f,
            0.35f,
            0.28f,
            0.22f,
            0.16f,
            0.10f
    };

    // Orange/soul colors (outer to center) - various shades of orange/amber
    private static final int[] BOX_COLORS = {
            0x8B4513, // Dark orange/brown (outermost)
            0xCD853F, // Peru/sandy brown
            0xDAA520, // Goldenrod
            0xFFA500, // Orange (middle)
            0xFFD700, // Gold
            0xFFFACD  // Lemon chiffon (center - light cream)
    };
    private static final float[] BOX_ALPHAS = {
            0.15f,   // Outer dark orange
            0.15f,   // Peru
            0.2f,    // Goldenrod  
            0.25f,   // Orange
            0.3f,    // Gold
            0.8f     // Light center
    };

    public SoulBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SoulBeamEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 entityPos = entity.position();
        Vec3 start = entity.getStart().subtract(entityPos);
        Vec3 end = entity.getEnd().subtract(entityPos);

        float length = (float) start.distanceTo(end);
        if (length < 0.01f) return;

        // Build smoother soul beam path (less jagged than lightning)
        Vec3[] points = new Vec3[SEGMENTS + 1];
        points[0] = start;
        points[SEGMENTS] = end;

        long seed = entity.getId() * 31L + entity.tickCount / 3; // Slower variation than lightning
        java.util.Random rand = new java.util.Random(seed);

        Vec3 direction = end.subtract(start).normalize();
        Vec3 perp = direction.cross(new Vec3(0, 1, 0));
        if (perp.lengthSqr() < 1e-3) perp = direction.cross(new Vec3(1, 0, 0));
        perp = perp.normalize();
        Vec3 up = perp.cross(direction).normalize();

        for (int i = 1; i < SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            Vec3 pos = start.lerp(end, t);

            float angle = rand.nextFloat() * (float) Math.PI * 2f;
            float offset = (rand.nextFloat() - 0.5f) * 2f * SPREAD;

            Vec3 randomOffset = perp.scale((float) Math.cos(angle) * offset)
                    .add(up.scale((float) Math.sin(angle) * offset));
            points[i] = pos.add(randomOffset);
        }

        // Render each segment (soul beam style)
        for (int i = 0; i < SEGMENTS; i++) {
            renderNestedBoxes(points[i], points[i + 1], entity, partialTicks, poseStack, buffer, packedLight);
        }
    }

    /** Renders nested, spinning boxes for a single soul beam segment */
    private void renderNestedBoxes(Vec3 from, Vec3 to, SoulBeamEntity entity, float partialTicks,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 diff = to.subtract(from);
        float segLength = (float) diff.length();
        if (segLength < 1e-8) return;

        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        poseStack.mulPose(lookAtQuaternion(diff));

        // Render all nested boxes
        for (int i = 0; i < BOX_WIDTHS.length; i++) {
            renderCuboid(poseStack, buffer, packedLight, segLength, BOX_WIDTHS[i], BOX_ALPHAS[i], BOX_COLORS[i]);
        }

        poseStack.popPose();
    }

    /** Draws a cuboid centered at 0,0 with width/height, length=segLength */
    private void renderCuboid(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                              float length, float width, float alpha, int color) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));

        float w = width / 2f;
        float h = width / 2f;
        float z0 = 0f;
        float z1 = length;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        addQuad(consumer, poseStack, -w, -h, z0,  w, -h, z0, w, -h, z1, -w, -h, z1, r, g, b, alpha, packedLight);
        addQuad(consumer, poseStack, -w,  h, z0, -w,  h, z1, w,  h, z1,  w,  h, z0, r, g, b, alpha, packedLight);
        addQuad(consumer, poseStack, -w, -h, z0, -w,  h, z0, -w,  h, z1, -w, -h, z1, r, g, b, alpha, packedLight);
        addQuad(consumer, poseStack, w, -h, z0,  w, -h, z1, w,  h, z1,  w,  h, z0, r, g, b, alpha, packedLight);
        addQuad(consumer, poseStack, -w, -h, z0, -w,  h, z0, w,  h, z0,  w, -h, z0, r, g, b, alpha, packedLight);
        addQuad(consumer, poseStack, -w, -h, z1,  w, -h, z1, w,  h, z1, -w,  h, z1, r, g, b, alpha, packedLight);
    }

    private void addQuad(VertexConsumer consumer, PoseStack poseStack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float r, float g, float b, float a, int light) {
        consumer.vertex(poseStack.last().pose(), x1, y1, z1)
                .color(r, g, b, a)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), x2, y2, z2)
                .color(r, g, b, a)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), x3, y3, z3)
                .color(r, g, b, a)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), x4, y4, z4)
                .color(r, g, b, a)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();
    }

    private static org.joml.Quaternionf lookAtQuaternion(Vec3 direction) {
        Vec3 from = new Vec3(0, 0, 1);
        Vec3 to = direction.normalize();
        double dot = from.dot(to);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < 1e-8) {
            if (dot > 0.9999) return new Quaternionf();
            else return new Quaternionf().rotationXYZ((float) Math.PI, 0, 0);
        }
        axis = axis.normalize();
        float angle = (float) Math.acos(dot);
        float half = angle / 2f;
        float s = (float) Math.sin(half);
        float w = (float) Math.cos(half);
        return new Quaternionf(axis.x * s, axis.y * s, axis.z * s, w);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulBeamEntity entity) {
        return BEAM_TEXTURE;
    }
}