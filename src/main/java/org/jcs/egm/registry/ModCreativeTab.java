package org.jcs.egm.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.egm;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, egm.MODID);

    public static final RegistryObject<CreativeModeTab> EGM_TAB =
            CREATIVE_TABS.register("egm_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("End Game"))
                            .icon(() -> new ItemStack(ModItems.GAUNTLET_ICON.get()))
                            .displayItems((params, output) -> {
                                ModItems.ITEMS.getEntries().forEach(item -> {
                                    // Exclude the gauntlet icon from appearing in creative menu
                                    if (item.get() != ModItems.GAUNTLET_ICON.get()) {
                                        output.accept(item.get());
                                    }
                                });
                            })
                            .build()
            );
    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }
}
