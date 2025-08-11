package org.jcs.egm.stones.stone_soul;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jcs.egm.registry.ModParticles;
import org.jcs.egm.stones.IGStoneAbility;
import org.jcs.egm.stones.StoneAbilityCooldowns;
import org.jcs.egm.stones.StoneItem;
import org.jcs.egm.stones.StoneUseDamage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SoulBindSoulStoneAbility implements IGStoneAbility {

    @Override
    public String abilityKey() { return "soul_bind"; }

    private static final int RADIUS  = 10; // 21×21 area
    private static final int Y_RANGE = 3;  // 3 up/down

    @Override
    public void activate(Level level, Player player, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return;

        boolean inGauntlet = StoneItem.isInGauntlet(player, stack);

        Item cdItem = StoneAbilityCooldowns.pickCooldownItem(player, stack);
        if (StoneAbilityCooldowns.isCooling(player, cdItem)) return;

        if (!inGauntlet) player.hurt(StoneUseDamage.get(server, player), 4.0F);

        server.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.7F, 1.2F);

        BlockPos center = player.blockPosition();
        AABB area = new AABB(
                center.getX() - RADIUS, center.getY() - Y_RANGE, center.getZ() - RADIUS,
                center.getX() + RADIUS + 1, center.getY() + Y_RANGE + 1, center.getZ() + RADIUS + 1
        );

        Set<BlockPos> effectPositions = new HashSet<>();
        boolean warned = false;

        ResourceLocation soulRealmKey = new ResourceLocation("egm", "soul_realm");
        ServerLevel soulRealm = server.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, soulRealmKey));
        if (soulRealm != null) {
            List<Player> targets = server.getEntitiesOfClass(Player.class, area);
            for (Player other : targets) {
                if (other.getUUID().equals(player.getUUID())) continue;
                if (other.level() == soulRealm) continue;
                other.changeDimension(soulRealm);
            }
        }

        List<LivingEntity> mobs = server.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity mob : mobs) {
            if (!(mob.isAlive()) || mob instanceof Player) continue;

            ItemStack egg = findSpawnEgg(mob.getType());
            if (egg.isEmpty()) {
                if (!warned) {
                    player.displayClientMessage(
                            Component.literal("A soul-less creature is nearby")
                                    .withStyle(Style.EMPTY.withItalic(true).withColor(0xD5700E)), true);
                    warned = true;
                }
                continue;
            }

            BlockPos pos = mob.blockPosition();
            mob.remove(RemovalReason.DISCARDED);

            ItemEntity dropped = new ItemEntity(
                    server,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    egg
            );
            dropped.setDeltaMovement(0.0, 0.01, 0.0);
            CompoundTag tag = dropped.getPersistentData();
            tag.putBoolean("SoulStoneNewEgg", true);
            server.addFreshEntity(dropped);

            effectPositions.add(pos);
        }

        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class, area);
        for (ItemEntity ie : items) {
            CompoundTag pd = ie.getPersistentData();
            if (pd.getBoolean("SoulStoneNewEgg")) {
                pd.remove("SoulStoneNewEgg");
                continue;
            }

            ItemStack worldStack = ie.getItem();
            Item itm = worldStack.getItem();
            if (!(itm instanceof SpawnEggItem se)) continue;

            EntityType<?> mType = se.getType(worldStack.getTag());
            if (mType == null) {
                var eggId = ForgeRegistries.ITEMS.getKey(itm);
                if (eggId != null && eggId.getPath().endsWith("_spawn_egg")) {
                    String mid = eggId.getPath().replace("_spawn_egg", "");
                    mType = ForgeRegistries.ENTITY_TYPES.getValue(
                            new net.minecraft.resources.ResourceLocation(eggId.getNamespace(), mid)
                    );
                }
            }
            if (mType == null) {
                if (!warned) {
                    player.displayClientMessage(
                            Component.literal("A soul-less creature is nearby")
                                    .withStyle(Style.EMPTY.withItalic(true).withColor(0xD5700E)), true);
                    warned = true;
                }
                continue;
            }

            BlockPos p = ie.blockPosition();
            LivingEntity spawned = (LivingEntity) mType.create(server);
            if (spawned != null) {
                spawned.moveTo(
                        p.getX() + 0.5, p.getY(), p.getZ() + 0.5,
                        server.random.nextFloat() * 360F, 0.0F
                );
                server.addFreshEntity(spawned);

                worldStack.shrink(1);
                if (worldStack.isEmpty()) {
                    ie.remove(RemovalReason.DISCARDED);
                }
                effectPositions.add(p);
            }
        }

        for (BlockPos p : effectPositions) {
            server.sendParticles(
                    ModParticles.SOUL_STONE_EFFECT_ONE.get(),
                    p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5,
                    80, 0.2, 0.3, 0.2, 0.03
            );
        }

        StoneAbilityCooldowns.apply(player, cdItem, "soul", abilityKey());
    }

    private ItemStack findSpawnEgg(EntityType<?> type) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (key == null) return ItemStack.EMPTY;
        String target = key.getNamespace() + ":" + key.getPath() + "_spawn_egg";

        var rl = new net.minecraft.resources.ResourceLocation(target);
        if (ForgeRegistries.ITEMS.containsKey(rl)) {
            return new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
        }
        for (Item itm : ForgeRegistries.ITEMS) {
            if (itm instanceof SpawnEggItem se && se.getType(null) == type) {
                return new ItemStack(itm);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canHoldUse() {
        return false;
    }
}
