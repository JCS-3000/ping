package org.jcs.egm.stones.stone_reality;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerMorphRenderHandler {

    private static final Map<UUID, Entity> PUPPETS = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre evt) {
        Player p = evt.getEntity();
        p.getCapability(MorphData.CAP).ifPresent(state -> {
            if (state.mobId == null) return;
            if (p.level().getGameTime() >= state.expiresAt) return;

            Entity puppet = PUPPETS.compute(p.getUUID(), (id, existing) -> {
                if (existing != null && existing.isAlive()
                        && ForgeRegistries.ENTITY_TYPES.getKey(existing.getType()).equals(state.mobId)) {
                    return existing;
                }
                // Recreate puppet entity of the desired type (client-only)
                EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(state.mobId);
                if (t == null) return null;
                Entity e = t.create(p.level());
                return e;
            });

            if (puppet == null) return;

            // Cancel normal player rendering
            evt.setCanceled(true);

            // Position puppet at player
            copyTransformFromPlayer(puppet, p);

            // Render puppet
            PoseStack ps = evt.getPoseStack();
            MultiBufferSource buffers = evt.getMultiBufferSource();
            int light = evt.getPackedLight();

            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            try {
                dispatcher.render(puppet, 0, 0, 0, p.getYRot(), evt.getPartialTick(), ps, buffers, light);
            } catch (Throwable ignored) {}
        });
    }

    private static void copyTransformFromPlayer(Entity puppet, Player p) {
        puppet.setPos(p.getX(), p.getY(), p.getZ());
        puppet.setYRot(p.getYRot());
        puppet.setXRot(p.getXRot());
        puppet.yRotO = p.yRotO;
        puppet.xRotO = p.xRotO;
        puppet.setDeltaMovement(p.getDeltaMovement());
        puppet.setOnGround(p.onGround());
        // Optional: handle swimming/crouching animation states if desired
    }
}
