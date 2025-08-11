package org.jcs.egm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jcs.egm.entity.TimeBubbleFieldEntity;

@OnlyIn(Dist.CLIENT)
public class TimeBubbleFieldRenderer extends EntityRenderer<TimeBubbleFieldEntity> {
    private static final ResourceLocation DUMMY = MissingTextureAtlasSprite.getLocation();

    public TimeBubbleFieldRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(TimeBubbleFieldEntity e, float yaw, float pt, PoseStack ps,
                       MultiBufferSource buf, int light) {
        // no-op: the entity is invisible; visuals are spawned as particles in its tick()
    }

    @Override
    public ResourceLocation getTextureLocation(TimeBubbleFieldEntity e) { return DUMMY; }
}
