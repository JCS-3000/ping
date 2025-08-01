package org.jcs.egm.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.egm;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.StoneHolderItem;
import org.jcs.egm.stones.StoneItem;


public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, egm.MODID);

    // THE STONES

    public static final RegistryObject<Item> MIND_STONE =
            ITEMS.register(
                    "mind_stone", () -> new StoneItem("mind",
                            0xffdd00, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> POWER_STONE =
            ITEMS.register(
                    "power_stone", () -> new StoneItem("power",
                            0x75179C, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SPACE_STONE =
            ITEMS.register(
                    "space_stone", () -> new StoneItem("space",
                            0x283095, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> REALITY_STONE =
            ITEMS.register(
                    "reality_stone", () -> new StoneItem("reality",
                            0xD10018, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> TIME_STONE =
            ITEMS.register(
                    "time_stone", () -> new StoneItem("time",
                            0x085828, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SOUL_STONE =
            ITEMS.register(
                    "soul_stone", () -> new StoneItem("soul",
                            0xD5700E, new Item.Properties().stacksTo(1)));

    // THE GAUNTLET

    public static final RegistryObject<Item> INFINITY_GAUNTLET =
            ITEMS.register("infinity_gauntlet", () -> new InfinityGauntletItem(new
                    Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    // STONE HOLDERS

    // --- Stone Holders ---
    public static final RegistryObject<Item> MIND_STONE_HOLDER =
            ITEMS.register("lokian_scepter", () -> new StoneHolderItem("mind", new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> POWER_STONE_HOLDER =
            ITEMS.register("orb_of_morag", () -> new StoneHolderItem("power", new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SPACE_STONE_HOLDER =
            ITEMS.register("tesseract", () -> new StoneHolderItem("space", new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> REALITY_STONE_HOLDER =
            ITEMS.register("aether", () -> new StoneHolderItem("reality", new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SOUL_STONE_HOLDER =
            ITEMS.register("red_skull", () -> new StoneHolderItem("soul", new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TIME_STONE_HOLDER =
            ITEMS.register("eye_of_agamotto", () -> new StoneHolderItem("time", new Item.Properties().stacksTo(1)));

    // URU

    public static final RegistryObject<Item> INGOT_URU = ITEMS.register("ingot_uru",
            () -> new Item(new Item.Properties()
                    .rarity(Rarity.UNCOMMON)
                    .stacksTo(64)
            ));

}
