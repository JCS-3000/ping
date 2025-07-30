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
import org.jcs.egm.entity.PowerStoneLightningEntity;
import org.joml.Quaternionf;

import java.util.Random;

public class PowerStoneLightningRenderer extends EntityRenderer<PowerStoneLightningEntity> {
    private static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation("egm:textures/entity/placeholder.png");

    private static final int SEGMENTS = 5;
    private static final float SPREAD = 0.4f;

    public PowerStoneLightningRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(PowerStoneLightningEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 entityPos = entity.position();
        Vec3 start = entity.getStart().subtract(entityPos);
        Vec3 end = entity.getEnd().subtract(entityPos);

        float length = (float) start.distanceTo(end);
        if (length < 0.01f) return;

        // Build lightning path with jagged randomization
        Vec3[] points = new Vec3[SEGMENTS + 1];
        points[0] = start;
        points[SEGMENTS] = end;

        long seed = entity.getId() * 31L + entity.tickCount / 2;
        Random rand = new Random(seed);

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

        // Render each segment (jagged lightning style)
        for (int i = 0; i < SEGMENTS; i++) {
            renderLayeredSegment(points[i], points[i + 1], poseStack, buffer, packedLight);
        }
    }

    private void renderLayeredSegment(Vec3 from, Vec3 to, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 diff = to.subtract(from);
        float segLength = (float) diff.length();
        if (segLength < 1e-8) return;

        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        poseStack.mulPose(lookAtQuaternion(diff));

        // Outer: Blueish purple (wider, more transparent)
        renderCuboid(poseStack, buffer, packedLight, segLength, 0.45f, 0.3f, 0x6a34eb); // blue-purple, alpha 0.3
        // Middle: Red-ish purple (medium, less transparent)
        renderCuboid(poseStack, buffer, packedLight, segLength, 0.29f, 0.7f, 0xbf249c); // red-purple, alpha 0.7
        // Core: White (thickest, opaque, drawn last so it's not tinted by others)
        renderCuboid(poseStack, buffer, packedLight, segLength, 0.17f, 1.0f, 0xffffff); // white, alpha 1.0

        poseStack.popPose();
    }

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

    private static Quaternionf lookAtQuaternion(Vec3 direction) {
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
    public ResourceLocation getTextureLocation(PowerStoneLightningEntity entity) {
        return BEAM_TEXTURE;
    }
}
