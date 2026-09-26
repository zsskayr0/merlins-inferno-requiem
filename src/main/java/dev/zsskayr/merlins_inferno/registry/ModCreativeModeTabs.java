package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
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
                        output.accept(ModItems.ASHWOOD_SAPLING_ITEM.get());
                        output.accept(ModItems.ASHWOOD_LOG_ITEM.get());
                        output.accept(ModItems.ASHWOOD_WOOD_ITEM.get());
                        output.accept(ModItems.ASHWOOD_PLANKS_ITEM.get());
                        output.accept(ModItems.ASHWOOD_STAIRS_ITEM.get());
                        output.accept(ModItems.ASHWOOD_SLAB_ITEM.get());
                        output.accept(ModItems.ASHWOOD_FENCE_ITEM.get());
                        output.accept(ModItems.ASHWOOD_FENCE_GATE_ITEM.get());
                        output.accept(ModItems.ASHWOOD_DOOR_ITEM.get());
                        output.accept(ModItems.ASHWOOD_TRAPDOOR_ITEM.get());
                        output.accept(ModItems.ASHWOOD_PRESSURE_PLATE_ITEM.get());
                        output.accept(ModItems.ASHWOOD_BUTTON_ITEM.get());
                        output.accept(ModItems.ASHWOOD_SIGN_ITEM.get());
                        output.accept(ModItems.ASHWOOD_HANGING_SIGN_ITEM.get());
                        output.accept(ModItems.STRIPPED_ASHWOOD_LOG_ITEM.get());
                        output.accept(ModItems.STRIPPED_ASHWOOD_WOOD_ITEM.get());
                        output.accept(ModItems.ASHWOOD_LEAVES_ITEM.get());
                        output.accept(ModItems.ROWANWOOD_LOG_ITEM.get());
                        output.accept(ModItems.ROWANWOOD_LEAVES_ITEM.get());
                        output.accept(ModItems.ROWANWOOD_BAR.get());
                        output.accept(ModItems.ROWANWOOD_SCRAP.get());
                        output.accept(ModItems.OTHERWORLD_ESSENCE.get());
                        output.accept(ModItems.MUNDANE_ESSENCE.get());
                        output.accept(ModItems.FAE_ESSENCE.get());
                        output.accept(ModItems.ROWANWOOD_SWORD.get());
                        output.accept(ModItems.ROWANWOOD_AXE.get());
                        output.accept(ModItems.ROWANWOOD_PICKAXE.get());
                        output.accept(ModItems.ROWANWOOD_SHOVEL.get());
                        output.accept(ModItems.ROWANWOOD_HOE.get());
                        output.accept(ModItems.ROWANWOOD_HELMET.get());
                        output.accept(ModItems.ROWANWOOD_CHESTPLATE.get());
                        output.accept(ModItems.ROWANWOOD_LEGGINGS.get());
                        output.accept(ModItems.ROWANWOOD_BOOTS.get());
                        output.accept(ModItems.DEMON_BLOOD.get());
                        output.accept(ModItems.INFERNAL_SINEW.get());
                        output.accept(ModItems.WITHERED_BONE.get());
                        output.accept(ModItems.DEMONBLOOD_SCRAP.get());
                        output.accept(ModItems.DEMONBLOOD_BAR.get());
                        output.accept(ModItems.DEMONBLOOD_SWORD.get());
                        output.accept(ModItems.DEMONBLOOD_AXE.get());
                        output.accept(ModItems.DEMONBLOOD_PICKAXE.get());
                        output.accept(ModItems.DEMONBLOOD_SHOVEL.get());
                        output.accept(ModItems.DEMONBLOOD_HOE.get());
                        output.accept(ModItems.DEMONBLOOD_HELMET.get());
                        output.accept(ModItems.DEMONBLOOD_CHESTPLATE.get());
                        output.accept(ModItems.DEMONBLOOD_LEGGINGS.get());
                        output.accept(ModItems.DEMONBLOOD_BOOTS.get());
                        output.accept(ModItems.DRUID_SPAWN_EGG.get());
                        output.accept(ModItems.IMP_SPAWN_EGG.get());
                        output.accept(ModItems.DULLAHAN_SPAWN_EGG.get());
                        output.accept(ModItems.INFERNAL_ESSENCE.get());
                        output.accept(ModItems.COMPRESSED_NETHERRACK_ITEM.get());
                        output.accept(ModItems.HELL_FORGE_ITEM.get());
                        output.accept(ModItems.LYRIUM_SHARD.get());
                        output.accept(ModItems.LYRIUM_ROD.get());
                        output.accept(ModItems.LYRIUM_RAW.get());
                        output.accept(ModItems.LYRIUM_IMPURE.get());
                        output.accept(ModItems.LYRIUM_REFINED.get());
                        output.accept(ModItems.LYRIUM_PURE.get());
                        output.accept(ModItems.LYRIUM_TRUE.get());
                        output.accept(ModItems.CELESTIAL_ESSENCE.get());
                        output.accept(ModItems.SERAPHIUM_SCRAP.get());
                        output.accept(ModItems.SERAPHIUM_BAR.get());
                        output.accept(ModItems.SERAPHIUM_SWORD.get());
                        output.accept(ModItems.SERAPHIUM_AXE.get());
                        output.accept(ModItems.SERAPHIUM_PICKAXE.get());
                        output.accept(ModItems.SERAPHIUM_SHOVEL.get());
                        output.accept(ModItems.SERAPHIUM_HOE.get());
                        output.accept(ModItems.SERAPHIUM_HELMET.get());
                        output.accept(ModItems.SERAPHIUM_CHESTPLATE.get());
                        output.accept(ModItems.SERAPHIUM_LEGGINGS.get());
                        output.accept(ModItems.SERAPHIUM_BOOTS.get());

                        // Enchanted books at max level - the only way to get one of these
                        // otherwise is the enchanting table/anvil RNG, or an /enchant command.
                        HolderLookup.RegistryLookup<Enchantment> enchantments = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT);
                        acceptEnchantedBook(output, enchantments, ModEnchantments.BANE_OF_HUMANITY, 5);
                        acceptEnchantedBook(output, enchantments, ModEnchantments.EVIL, 5);
                        acceptEnchantedBook(output, enchantments, ModEnchantments.DRUIDS_TOUCH, 1);
                    })
                    .build());

    private static void acceptEnchantedBook(CreativeModeTab.Output output, HolderLookup.RegistryLookup<Enchantment> enchantments,
            ResourceKey<Enchantment> key, int level) {
        enchantments.get(key).ifPresent(holder -> {
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, level));
            output.accept(book);
        });
    }

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
