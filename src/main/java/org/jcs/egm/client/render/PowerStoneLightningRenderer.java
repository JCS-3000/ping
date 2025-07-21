package org.jcs.egm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.entity.PowerStoneLightningEntity;

import java.util.Random;

public class PowerStoneLightningRenderer extends EntityRenderer<PowerStoneLightningEntity> {
    private static final ResourceLocation LIGHTNING_TEXTURE = new ResourceLocation("textures/entity/lightning.png");

    public PowerStoneLightningRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(PowerStoneLightningEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 start = entity.getStart();
        Vec3 end = entity.getEnd();
        if (start == null || end == null) return;

        // For fadeout: alpha goes from 1 -> 0 as entity dies
        float alpha = Mth.clamp(entity.tickCount / 10.0f, 0f, 1f);

        renderLightning(start, end, poseStack, buffer, packedLight, alpha);
    }

    private void renderLightning(Vec3 start, Vec3 end, PoseStack poseStack, MultiBufferSource buffer,
                                 int packedLight, float alpha) {
        final int segments = 12; // how many zig-zags
        final float thickness = 0.15f;

        Random rand = new Random();
        Vec3[] points = new Vec3[segments + 1];

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 pos = start.lerp(end, t);

            // Add some zig-zag (perpendicular to the main direction)
            if (i != 0 && i != segments) {
                Vec3 dir = end.subtract(start).normalize();
                Vec3 up = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                Vec3 right = dir.cross(up).normalize();
                Vec3 up2 = dir.cross(right).normalize();

                double radius = 0.25 + rand.nextDouble() * 0.15;
                double angle = rand.nextDouble() * Math.PI * 2;

                Vec3 offset = right.scale(Math.cos(angle) * radius).add(up2.scale(Math.sin(angle) * radius));
                pos = pos.add(offset);
            }
            points[i] = pos;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        for (int i = 0; i < segments; i++) {
            Vec3 a = points[i];
            Vec3 b = points[i + 1];
            float r = 0.45f, g = 0.7f, bl = 1f; // Lightning color

            // Simple quad for each segment (can be improved)
            consumer.vertex(a.x, a.y, a.z).color(r, g, bl, alpha).endVertex();
            consumer.vertex(b.x, b.y, b.z).color(r, g, bl, alpha).endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(PowerStoneLightningEntity entity) {
        return LIGHTNING_TEXTURE;
    }
}
