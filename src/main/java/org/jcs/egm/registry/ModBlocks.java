package org.jcs.egm.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.blocks.TrappedStarlightBlock;
import org.jcs.egm.egm;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, egm.MODID);

    // URU BLOCK - Epic rarity, higher blast resistance than obsidian
    public static final RegistryObject<Block> URU_BLOCK = BLOCKS.register("uru_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops()
                    .strength(50.0F, 2400.0F) // Higher than obsidian (1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)));

    // TRAPPED STARLIGHT - Rare ore that spawns under end islands
    public static final RegistryObject<Block> TRAPPED_STARLIGHT = BLOCKS.register("trapped_starlight",
            () -> new TrappedStarlightBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F) // Similar to diamond ore mining speed
                    .sound(SoundType.STONE)));

    // Block items
    public static final RegistryObject<Item> URU_BLOCK_ITEM = registerBlockItem("uru_block", URU_BLOCK, 
            () -> new Item.Properties().rarity(Rarity.EPIC));
    
    public static final RegistryObject<Item> TRAPPED_STARLIGHT_ITEM = registerBlockItem("trapped_starlight", TRAPPED_STARLIGHT, 
            () -> new Item.Properties().rarity(Rarity.RARE));

    // Helper method to register block items
    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block, Supplier<Item.Properties> properties) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), properties.get()));
    }
}