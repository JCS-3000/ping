package org.jcs.egm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.entity.PowerStoneLightningEntity;
import org.joml.Quaternionf;

public class PowerStoneLightningRenderer extends EntityRenderer<PowerStoneLightningEntity> {
    private static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation("egm:textures/entity/power_stone_beam.png");

    public PowerStoneLightningRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(PowerStoneLightningEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 start = entity.getStart();
        Vec3 end   = entity.getEnd();
        if (start == null || end == null) return;

        Vec3 diff = end.subtract(start);
        float length = (float) diff.length();
        if (length < 0.01f) return;

        poseStack.pushPose();
        // Align beam
        poseStack.mulPose(lookAtQuaternion(diff));
        // Spin effect
        float spin = (entity.tickCount + partialTicks) * 5f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        // Static UV
        float scrollV = 0.0f;

        renderBeaconCuboid(poseStack, buffer, packedLight, length, 0.32f, 1f, scrollV);
        poseStack.popPose();
    }

    /**
     * Rotates local Z+ to direction.
     */
    private static Quaternionf lookAtQuaternion(Vec3 direction) {
        Vec3 from = new Vec3(0, 0, 1);
        Vec3 to   = direction.normalize();
        Vec3 axis = from.cross(to);
        double angle = Math.acos(from.dot(to));
        if (axis.lengthSqr() < 1e-6) {
            if (from.dot(to) > 0) {
                return new Quaternionf();
            } else {
                return new Quaternionf().rotationXYZ((float)Math.PI, 0, 0);
            }
        }
        axis = axis.normalize();
        float half = (float)(angle / 2);
        float s    = (float)Math.sin(half);
        float w    = (float)Math.cos(half);
        return new Quaternionf(axis.x * s, axis.y * s, axis.z * s, w);
    }

    /**
     * Draws a beam cuboid from z=0 to z=length on the local Z axis.
     */
    private void renderBeaconCuboid(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                    float length, float width, float alpha, float scrollV) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));

        float w = width / 2f;
        float h = width / 2f;
        float z0 = 0f;
        float z1 = length;

        // UVs
        float u0 = 0f, u1 = 1f;
        float v0 = 0f;
        float v1 = length / 8f;

        float pr = 0.8f, pg = 0.1f, pb = 1.0f; // purple tint

        // Bottom
        addQuad(consumer, poseStack,
                -w, -h, z0,  w, -h, z0,
                w, -h, z1, -w, -h, z1,
                u0, v0, u1, v0, u1, v1, u0, v1,
                pr, pg, pb, alpha, packedLight);
        // Top
        addQuad(consumer, poseStack,
                -w,  h, z0, -w,  h, z1,
                w,  h, z1,  w,  h, z0,
                u0, v0, u0, v1, u1, v1, u1, v0,
                pr, pg, pb, alpha, packedLight);
        // North
        addQuad(consumer, poseStack,
                -w, -h, z0, -w,  h, z0,
                -w,  h, z1, -w, -h, z1,
                u0, v0, u0, v1, u1, v1, u1, v0,
                pr, pg, pb, alpha, packedLight);
        // South
        addQuad(consumer, poseStack,
                w, -h, z0,  w, -h, z1,
                w,  h, z1,  w,  h, z0,
                u0, v0, u1, v0, u1, v1, u0, v1,
                pr, pg, pb, alpha, packedLight);
        // West
        addQuad(consumer, poseStack,
                -w, -h, z0, -w,  h, z0,
                w,  h, z0,  w, -h, z0,
                u0, v0, u0, v1, u1, v1, u1, v0,
                pr, pg, pb, alpha, packedLight);
        // East
        addQuad(consumer, poseStack,
                -w, -h, z1,  w, -h, z1,
                w,  h, z1, -w,  h, z1,
                u0, v0, u1, v0, u1, v1, u0, v1,
                pr, pg, pb, alpha, packedLight);
    }

    private void addQuad(VertexConsumer consumer, PoseStack poseStack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float u1, float v1, float u2, float v2,
                         float u3, float v3, float u4, float v4,
                         float r, float g, float b, float a, int light) {
        consumer.vertex(poseStack.last().pose(), x1, y1, z1)
                .color(r, g, b, a)
                .uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), x2, y2, z2)
                .color(r, g, b, a)
                .uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), x3, y3, z3)
                .color(r, g, b, a)
                .uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), x4, y4, z4)
                .color(r, g, b, a)
                .uv(u4, v4)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(PowerStoneLightningEntity entity) {
        return BEAM_TEXTURE;
    }
}
