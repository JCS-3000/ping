package org.jcs.egm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jcs.egm.entity.SingularityEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SingularityRenderer extends EntityRenderer<SingularityEntity> {

    private static final ResourceLocation WHITE_TEX =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    // Fast but readable spin
    private static final float YAW_DEG_PER_TICK  = 36f;
    private static final float ROLL_DEG_PER_TICK = 24f;

    public SingularityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(SingularityEntity e, float entityYaw, float pt,
                       PoseStack ps, MultiBufferSource buf, int packedLight) {
        ps.pushPose();

        // Quarter-size cube
        ps.scale(0.25f, 0.25f, 0.25f);

        // Spin
        float t = e.getAge() + pt;
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(t * YAW_DEG_PER_TICK));
        ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(t * ROLL_DEG_PER_TICK));

        // Solid black (opaque)
        float r = 0f, g = 0f, b = 0f, a = 1f;

        Matrix4f pose = ps.last().pose();
        Matrix3f nMat = ps.last().normal();

        // Use solid entity pipeline (pos, color, uv, overlay, light, normal)
        VertexConsumer vc = buf.getBuffer(RenderType.entitySolid(WHITE_TEX));

        final float h = 0.5f;

        // Each face defined explicitly with outward CCW winding
        // Then we also emit the same face reversed (double-sided) to totally eliminate cull artifacts.

        // Z+ (0,0,1)
        addQuad(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f(-h,-h, h), new Vector3f( h,-h, h),
                new Vector3f( h, h, h), new Vector3f(-h, h, h),
                new Vector3f(0,0,1));
        addQuadReversed(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f(-h,-h, h), new Vector3f( h,-h, h),
                new Vector3f( h, h, h), new Vector3f(-h, h, h),
                new Vector3f(0,0,1));

        // Z- (0,0,-1)
        addQuad(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f( h,-h,-h), new Vector3f(-h,-h,-h),
                new Vector3f(-h, h,-h), new Vector3f( h, h,-h),
                new Vector3f(0,0,-1));
        addQuadReversed(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f( h,-h,-h), new Vector3f(-h,-h,-h),
                new Vector3f(-h, h,-h), new Vector3f( h, h,-h),
                new Vector3f(0,0,-1));

        // X+ (1,0,0)
        addQuad(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f( h,-h, h), new Vector3f( h,-h,-h),
                new Vector3f( h, h,-h), new Vector3f( h, h, h),
                new Vector3f(1,0,0));
        addQuadReversed(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f( h,-h, h), new Vector3f( h,-h,-h),
                new Vector3f( h, h,-h), new Vector3f( h, h, h),
                new Vector3f(1,0,0));

        // X- (-1,0,0)
        addQuad(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f(-h,-h,-h), new Vector3f(-h,-h, h),
                new Vector3f(-h, h, h), new Vector3f(-h, h,-h),
                new Vector3f(-1,0,0));
        addQuadReversed(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f(-h,-h,-h), new Vector3f(-h,-h, h),
                new Vector3f(-h, h, h), new Vector3f(-h, h,-h),
                new Vector3f(-1,0,0));

        // Y+ (0,1,0)
        addQuad(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f(-h, h, h), new Vector3f( h, h, h),
                new Vector3f( h, h,-h), new Vector3f(-h, h,-h),
                new Vector3f(0,1,0));
        addQuadReversed(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f(-h, h, h), new Vector3f( h, h, h),
                new Vector3f( h, h,-h), new Vector3f(-h, h,-h),
                new Vector3f(0,1,0));

        // Y- (0,-1,0)
        addQuad(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f(-h,-h,-h), new Vector3f( h,-h,-h),
                new Vector3f( h,-h, h), new Vector3f(-h,-h, h),
                new Vector3f(0,-1,0));
        addQuadReversed(vc, pose, nMat, r,g,b,a, packedLight,
                new Vector3f(-h,-h,-h), new Vector3f( h,-h,-h),
                new Vector3f( h,-h, h), new Vector3f(-h,-h, h),
                new Vector3f(0,-1,0));

        ps.popPose();
        super.render(e, entityYaw, pt, ps, buf, packedLight);
    }

    private static void addQuad(VertexConsumer vc, Matrix4f pose, Matrix3f nMat,
                                float r, float g, float b, float a, int light,
                                Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f n) {
        Vector3f nt = new Vector3f(
                n.x * nMat.m00() + n.y * nMat.m10() + n.z * nMat.m20(),
                n.x * nMat.m01() + n.y * nMat.m11() + n.z * nMat.m21(),
                n.x * nMat.m02() + n.y * nMat.m12() + n.z * nMat.m22()
        ).normalize();

        put(vc, pose, v0, r,g,b,a, 0f,0f, light, nt);
        put(vc, pose, v1, r,g,b,a, 1f,0f, light, nt);
        put(vc, pose, v2, r,g,b,a, 1f,1f, light, nt);

        put(vc, pose, v2, r,g,b,a, 1f,1f, light, nt);
        put(vc, pose, v3, r,g,b,a, 0f,1f, light, nt);
        put(vc, pose, v0, r,g,b,a, 0f,0f, light, nt);
    }

    // Emit the same face but with reversed winding/normal (back-face), so culling never hides it.
    private static void addQuadReversed(VertexConsumer vc, Matrix4f pose, Matrix3f nMat,
                                        float r, float g, float b, float a, int light,
                                        Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f n) {
        Vector3f nt = new Vector3f(
                -n.x * nMat.m00() + -n.y * nMat.m10() + -n.z * nMat.m20(),
                -n.x * nMat.m01() + -n.y * nMat.m11() + -n.z * nMat.m21(),
                -n.x * nMat.m02() + -n.y * nMat.m12() + -n.z * nMat.m22()
        ).normalize();

        // reversed order: v0,v3,v2 and v2,v1,v0
        put(vc, pose, v0, r,g,b,a, 0f,0f, light, nt);
        put(vc, pose, v3, r,g,b,a, 0f,1f, light, nt);
        put(vc, pose, v2, r,g,b,a, 1f,1f, light, nt);

        put(vc, pose, v2, r,g,b,a, 1f,1f, light, nt);
        put(vc, pose, v1, r,g,b,a, 1f,0f, light, nt);
        put(vc, pose, v0, r,g,b,a, 0f,0f, light, nt);
    }

    private static void put(VertexConsumer vc, Matrix4f pose, Vector3f p,
                            float r, float g, float b, float a,
                            float u, float v, int light, Vector3f n) {
        vc.vertex(pose, p.x, p.y, p.z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n.x, n.y, n.z)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SingularityEntity entity) {
        return WHITE_TEX;
    }
}
