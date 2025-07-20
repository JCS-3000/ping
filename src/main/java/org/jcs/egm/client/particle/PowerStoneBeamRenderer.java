package org.jcs.egm.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jcs.egm.entity.PowerStoneBeamEntity;


public class PowerStoneBeamRenderer extends EntityRenderer<PowerStoneBeamEntity> {
    public static final ResourceLocation BEAM_TEX = new ResourceLocation("minecraft:textures/entity/beacon_beam.png");

    public PowerStoneBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(PowerStoneBeamEntity beam, float yaw, float partialTicks, PoseStack stack, MultiBufferSource buffers, int light) {
        Vec3 from = beam.getStart();
        Vec3 to = beam.getEnd();

        if (from == null || to == null) return;

        // Basic vanilla-style straight beam (single quad), no animation
        float width = 0.25F;

        // Vector math
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

        stack.pushPose();

        // Move to the beam start position, relative to entity position
        double ex = from.x - beam.getX();
        double ey = from.y - beam.getY();
        double ez = from.z - beam.getZ();
        stack.translate(ex, ey, ez);

        // Rotate stack to point along the beam direction
        float yawAngle = (float) (Math.atan2(dx, dz) * 180 / Math.PI);
        float pitchAngle = (float) (Math.asin(dy / len) * 180 / Math.PI);
        stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yawAngle));
        stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-pitchAngle));


        // Render beam quad
        var builder = buffers.getBuffer(RenderType.beaconBeam(BEAM_TEX, false));
        float minU = 0.0F, maxU = 1.0F, minV = 0.0F, maxV = 1.0F;

        float halfW = width / 2.0F;
        float y0 = 0.0F, y1 = (float) len;

        // Vertices (beam is a vertical quad, centered on (0,0,0), pointing along +Y)
        builder.vertex(stack.last().pose(), -halfW, y0, 0).uv(minU, minV).endVertex();
        builder.vertex(stack.last().pose(), -halfW, y1, 0).uv(minU, maxV).endVertex();
        builder.vertex(stack.last().pose(), halfW, y1, 0).uv(maxU, maxV).endVertex();
        builder.vertex(stack.last().pose(), halfW, y0, 0).uv(maxU, minV).endVertex();

        stack.popPose();
        super.render(beam, yaw, partialTicks, stack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PowerStoneBeamEntity entity) {
        return BEAM_TEX;
    }
}
