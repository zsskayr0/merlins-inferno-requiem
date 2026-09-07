package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * The mod's own creative-mode tab.
 * <p>
 * Add new entries to {@code displayItems} as {@link ModItems}/{@link ModBlocks} grow (or listen
 * to {@code BuildCreativeModeTabContentsEvent} instead, for cases that need runtime logic).
 */
public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Merlins_inferno.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MERLINS_INFERNO_TAB = CREATIVE_MODE_TABS.register("merlins_inferno_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.merlins_inferno"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.GRIMMORIUM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GRIMMORIUM.get());
                        output.accept(ModItems.IRONWOOD_BAR.get());
                        output.accept(ModItems.IRONWOOD_SWORD.get());
                        output.accept(ModItems.IRONWOOD_AXE.get());
                        output.accept(ModItems.IRONWOOD_SHOVEL.get());
                        output.accept(ModItems.IRONWOOD_HOE.get());
                        output.accept(ModItems.IRONWOOD_HELMET.get());
                        output.accept(ModItems.IRONWOOD_CHESTPLATE.get());
                        output.accept(ModItems.IRONWOOD_LEGGINGS.get());
                        output.accept(ModItems.IRONWOOD_BOOTS.get());
                    })
                    .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
