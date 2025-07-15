package org.jcs.egm.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.gauntlet.InfinityGauntletMenu;
import org.jcs.egm.egm;
import net.minecraft.world.item.ItemStack;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, egm.MODID);

    public static final RegistryObject<MenuType<InfinityGauntletMenu>> INFINITY_GAUNTLET =
            MENUS.register("infinity_gauntlet", () ->
                    IForgeMenuType.create((id, inv, buf) -> {
                        ItemStack stack = buf.readItem();
                        return new InfinityGauntletMenu(id, inv, stack);
                    })
            );
}