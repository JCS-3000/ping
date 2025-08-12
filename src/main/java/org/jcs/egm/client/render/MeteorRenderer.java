package org.jcs.egm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jcs.egm.entity.MeteorEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class MeteorRenderer extends EntityRenderer<MeteorEntity> {

    private static final ResourceLocation STONE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/stone.png");
    private static final ResourceLocation BLACKSTONE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/blackstone.png");

    // Spin speed for the meteor
    private static final float YAW_DEG_PER_TICK = 45f;
    private static final float PITCH_DEG_PER_TICK = 30f;

    public MeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(MeteorEntity meteor, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Make it a decent size
        poseStack.scale(1.5f, 1.5f, 1.5f);

        // Spin the meteor
        float time = meteor.tickCount + partialTick;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * YAW_DEG_PER_TICK));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(time * PITCH_DEG_PER_TICK));

        // Choose texture based on meteor type
        ResourceLocation texture = meteor.isStone() ? STONE_TEXTURE : BLACKSTONE_TEXTURE;
        
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Use cutout render type for better visibility
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(texture));

        // Render a cube
        renderCube(vertexConsumer, pose, normal, packedLight);

        poseStack.popPose();
        super.render(meteor, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderCube(VertexConsumer vc, Matrix4f pose, Matrix3f normal, int packedLight) {
        final float size = 0.5f;
        final float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f; // White tint

        // Front face (Z+)
        addQuad(vc, pose, normal, r, g, b, a, packedLight,
                new Vector3f(-size, -size, size), new Vector3f(size, -size, size),
                new Vector3f(size, size, size), new Vector3f(-size, size, size),
                new Vector3f(0, 0, 1));

        // Back face (Z-)
        addQuad(vc, pose, normal, r, g, b, a, packedLight,
                new Vector3f(size, -size, -size), new Vector3f(-size, -size, -size),
                new Vector3f(-size, size, -size), new Vector3f(size, size, -size),
                new Vector3f(0, 0, -1));

        // Right face (X+)
        addQuad(vc, pose, normal, r, g, b, a, packedLight,
                new Vector3f(size, -size, size), new Vector3f(size, -size, -size),
                new Vector3f(size, size, -size), new Vector3f(size, size, size),
                new Vector3f(1, 0, 0));

        // Left face (X-)
        addQuad(vc, pose, normal, r, g, b, a, packedLight,
                new Vector3f(-size, -size, -size), new Vector3f(-size, -size, size),
                new Vector3f(-size, size, size), new Vector3f(-size, size, -size),
                new Vector3f(-1, 0, 0));

        // Top face (Y+)
        addQuad(vc, pose, normal, r, g, b, a, packedLight,
                new Vector3f(-size, size, size), new Vector3f(size, size, size),
                new Vector3f(size, size, -size), new Vector3f(-size, size, -size),
                new Vector3f(0, 1, 0));

        // Bottom face (Y-)
        addQuad(vc, pose, normal, r, g, b, a, packedLight,
                new Vector3f(-size, -size, -size), new Vector3f(size, -size, -size),
                new Vector3f(size, -size, size), new Vector3f(-size, -size, size),
                new Vector3f(0, -1, 0));
    }

    private static void addQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normalMatrix,
                               float r, float g, float b, float a, int packedLight,
                               Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f normalVec) {
        // Transform normal
        Vector3f transformedNormal = new Vector3f(
                normalVec.x * normalMatrix.m00() + normalVec.y * normalMatrix.m10() + normalVec.z * normalMatrix.m20(),
                normalVec.x * normalMatrix.m01() + normalVec.y * normalMatrix.m11() + normalVec.z * normalMatrix.m21(),
                normalVec.x * normalMatrix.m02() + normalVec.y * normalMatrix.m12() + normalVec.z * normalMatrix.m22()
        ).normalize();

        // First triangle
        addVertex(vc, pose, v0, r, g, b, a, 0f, 0f, packedLight, transformedNormal);
        addVertex(vc, pose, v1, r, g, b, a, 1f, 0f, packedLight, transformedNormal);
        addVertex(vc, pose, v2, r, g, b, a, 1f, 1f, packedLight, transformedNormal);

        // Second triangle
        addVertex(vc, pose, v2, r, g, b, a, 1f, 1f, packedLight, transformedNormal);
        addVertex(vc, pose, v3, r, g, b, a, 0f, 1f, packedLight, transformedNormal);
        addVertex(vc, pose, v0, r, g, b, a, 0f, 0f, packedLight, transformedNormal);
    }

    private static void addVertex(VertexConsumer vc, Matrix4f pose, Vector3f pos,
                                 float r, float g, float b, float a,
                                 float u, float v, int packedLight, Vector3f normal) {
        vc.vertex(pose, pos.x, pos.y, pos.z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal.x, normal.y, normal.z)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity meteor) {
        return meteor.isStone() ? STONE_TEXTURE : BLACKSTONE_TEXTURE;
    }
}