package org.jcs.egm.stones.stone_soul;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.ITeleporter;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.stones.StoneItem;
import org.jcs.egm.stones.StoneUseDamage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SoulBindSoulStoneAbility implements IGStoneAbility {

    @Override public String abilityKey() { return "soul_bind"; }
    @Override public boolean canHoldUse() { return false; }

    private static final int RADIUS  = 10; // 21×21 area
    private static final int Y_RANGE = 3;  // 3 up/down

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return;

        boolean inGauntlet = StoneItem.isInGauntlet(player, stack);

        // Cooldown API uses Item, not ItemStack
        Item cdItem = StoneAbilityCooldowns.pickCooldownItem(player, stack);
        if (StoneAbilityCooldowns.isCooling(player, cdItem)) return;

        if (!inGauntlet) player.hurt(StoneUseDamage.get(server, player), 4.0F);

        server.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.7F, 1.2F);

        BlockPos center = player.blockPosition();
        AABB area = new AABB(
                center.getX() - RADIUS, center.getY() - Y_RANGE, center.getZ() - RADIUS,
                center.getX() + RADIUS + 1, center.getY() + Y_RANGE + 1, center.getZ() + RADIUS + 1
        );

        // Resolve Soul Realm dimension
        ResourceKey<Level> soulKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("egm:soul_realm")
        );
        ServerLevel soulRealm = server.getServer().getLevel(soulKey);
        if (soulRealm == null) {
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Soul Realm is missing."), true);
            }
            return;
        }

        // Target in Soul Realm: caster's X/Z, safe terrain Y
        final double targetX = player.getX();
        final double targetZ = player.getZ();
        final double targetY = safeY(soulRealm, Mth.floor(targetX), Mth.floor(targetZ)) + 0.1;

        // Collect EVERYTHING except the caster, and anything already in Soul Realm
        List<Entity> toTeleport = new ArrayList<>(server.getEntitiesOfClass(Entity.class, area));
        toTeleport.removeIf(e -> e.getUUID().equals(player.getUUID()));
        toTeleport.removeIf(e -> e.level().dimension().equals(soulRealm.dimension()));

        // Visuals at origin
        for (Entity e : toTeleport) {
            server.sendParticles(
                    ModParticles.SOUL_STONE_EFFECT_ONE.get(),
                    e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
                    40, 0.2, 0.3, 0.2, 0.03
            );
        }

        // Teleport everyone (players via teleportTo, others via ITeleporter)
        for (Entity e : toTeleport) {
            teleportEntityTo(e, soulRealm, targetX + 0.5, targetY, targetZ + 0.5);
        }

        // Debug hint: where to look in the Soul Realm
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            String.format("SoulBind → egm:soul_realm @ (%.1f, %.1f, %.1f)",
                                    targetX + 0.5, targetY, targetZ + 0.5)
                    ), true
            );
        }

        StoneAbilityCooldowns.apply(player, cdItem, "soul", abilityKey());
    }

    /** Cross-dimension teleporter. Players use teleportTo; others use ITeleporter placement. */
    private static void teleportEntityTo(Entity e, ServerLevel dest, double x, double y, double z) {
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(dest, x, y, z, sp.getYRot(), sp.getXRot());
            return;
        }
        e.changeDimension(dest, new ITeleporter() {
            @Override
            public Entity placeEntity(Entity entity, ServerLevel current, ServerLevel destination,
                                      float yaw, Function<Boolean, Entity> repositionEntity) {
                Entity moved = repositionEntity.apply(false); // vanilla copy in dest
                if (moved != null) moved.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
                return moved;
            }
        });
    }

    private static int safeY(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight()) {
            y = level.getSharedSpawnPos().getY();
        }
        return y;
    }
}
