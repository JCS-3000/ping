package org.jcs.egm.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.blocks.entity.NidavelliranForgeBlockEntity;
import org.jcs.egm.egm;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, egm.MODID);

    public static final RegistryObject<BlockEntityType<NidavelliranForgeBlockEntity>> NIDAVELLIRIAN_FORGE =
            BLOCK_ENTITIES.register("nidavellirian_forge", () ->
                    BlockEntityType.Builder.of(NidavelliranForgeBlockEntity::new,
                            ModBlocks.NIDAVELLIRIAN_FORGE.get()).build(null));
}