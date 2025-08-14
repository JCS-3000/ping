package org.jcs.egm.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.egm;
import org.jcs.egm.gauntlet.InfinityGauntletItem;
import org.jcs.egm.holders.KreeWarhammerItem;
import org.jcs.egm.holders.LokianScepterItem;
import org.jcs.egm.holders.StoneHolderItem;
import org.jcs.egm.stones.stone_mind.MindStoneItem;
import org.jcs.egm.stones.stone_power.PowerStoneItem;
import org.jcs.egm.stones.stone_reality.RealityStoneItem;
import org.jcs.egm.stones.stone_soul.SoulStoneItem;
import org.jcs.egm.stones.stone_space.SpaceStoneItem;
import org.jcs.egm.stones.stone_time.TimeStoneItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, egm.MODID);

    // THE STONES (now each is its own class!)
    public static final RegistryObject<Item> MIND_STONE =
            ITEMS.register("mind_stone", () -> new MindStoneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> POWER_STONE =
            ITEMS.register("power_stone", () -> new PowerStoneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SPACE_STONE =
            ITEMS.register("space_stone", () -> new SpaceStoneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> REALITY_STONE =
            ITEMS.register("reality_stone", () -> new RealityStoneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TIME_STONE =
            ITEMS.register("time_stone", () -> new TimeStoneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SOUL_STONE =
            ITEMS.register("soul_stone", () -> new SoulStoneItem(new Item.Properties().stacksTo(1)));

    // THE GAUNTLET
    public static final RegistryObject<Item> INFINITY_GAUNTLET =
            ITEMS.register("infinity_gauntlet", () -> new InfinityGauntletItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)));

    // STONE HOLDERS
    public static final RegistryObject<Item> MIND_STONE_HOLDER =
            ITEMS.register("lokian_scepter", () -> new LokianScepterItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> POWER_STONE_HOLDER =
            ITEMS.register("kree_warhammer", () -> new KreeWarhammerItem(new Item.Properties().stacksTo(1)));
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
                    .rarity(Rarity.EPIC)
                    .stacksTo(64)
            ));

    // STARLIGHT CHUNK - Rare material from Trapped Starlight
    public static final RegistryObject<Item> STARLIGHT_CHUNK = ITEMS.register("starlight_chunk",
            () -> new Item(new Item.Properties()
                    .rarity(Rarity.RARE)
                    .stacksTo(64)
            ));

    // CREATIVE TAB ICON (not in creative menu)
    public static final RegistryObject<Item> GAUNTLET_ICON = ITEMS.register("gauntlet_icon",
            () -> new Item(new Item.Properties().stacksTo(1)));
}
