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
                        output.accept(ModItems.ASHWOOD_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.SPRUCE_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.OAK_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.BIRCH_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.JUNGLE_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.ACACIA_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.DARK_OAK_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.MANGROVE_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.CHERRY_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.BAMBOO_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.CRIMSON_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.WARPED_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.HAUNTED_GREAT_DOOR_ITEM.get());
                        output.accept(ModItems.ASHWOOD_TRAPDOOR_ITEM.get());
                        output.accept(ModItems.ASHWOOD_PRESSURE_PLATE_ITEM.get());
                        output.accept(ModItems.ASHWOOD_BUTTON_ITEM.get());
                        output.accept(ModItems.ASHWOOD_SIGN_ITEM.get());
                        output.accept(ModItems.ASHWOOD_HANGING_SIGN_ITEM.get());
                        output.accept(ModItems.STRIPPED_ASHWOOD_LOG_ITEM.get());
                        output.accept(ModItems.STRIPPED_ASHWOOD_WOOD_ITEM.get());
                        output.accept(ModItems.ASHWOOD_LEAVES_ITEM.get());
                        output.accept(ModItems.STRIPPED_HAUNTED_LOG_ITEM.get());
                        output.accept(ModItems.HAUNTED_WOOD_ITEM.get());
                        output.accept(ModItems.STRIPPED_HAUNTED_WOOD_ITEM.get());
                        output.accept(ModItems.HAUNTED_PLANKS_ITEM.get());
                        output.accept(ModItems.HAUNTED_STAIRS_ITEM.get());
                        output.accept(ModItems.HAUNTED_SLAB_ITEM.get());
                        output.accept(ModItems.HAUNTED_FENCE_ITEM.get());
                        output.accept(ModItems.HAUNTED_FENCE_GATE_ITEM.get());
                        output.accept(ModItems.HAUNTED_DOOR_ITEM.get());
                        output.accept(ModItems.HAUNTED_TRAPDOOR_ITEM.get());
                        output.accept(ModItems.HAUNTED_PRESSURE_PLATE_ITEM.get());
                        output.accept(ModItems.HAUNTED_BUTTON_ITEM.get());
                        output.accept(ModItems.HAUNTED_SIGN_ITEM.get());
                        output.accept(ModItems.HAUNTED_HANGING_SIGN_ITEM.get());
                        output.accept(ModItems.HAUNTED_BOAT.get());
                        output.accept(ModItems.ROWANWOOD_LOG_ITEM.get());
                        output.accept(ModItems.ROWANWOOD_LEAVES_ITEM.get());
                        output.accept(ModItems.ROWANWOOD_BAR.get());
                        output.accept(ModItems.ROWANWOOD_SCRAP.get());
                        output.accept(ModItems.OTHERWORLD_ESSENCE.get());
                        output.accept(ModItems.MUNDANE_ESSENCE.get());
                        output.accept(ModItems.FAE_ESSENCE.get());
                        output.accept(ModItems.ANTIDOTE.get());
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
                        output.accept(ModItems.DEVIL_FLESH.get());
                        output.accept(ModItems.MONSTER_LEATHER.get());
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
                        output.accept(ModItems.STARVED_SPAWN_EGG.get());
                        output.accept(ModItems.GRYMN_SPAWN_EGG.get());
                        output.accept(ModItems.DULLAHAN_SPAWN_EGG.get());
                        output.accept(ModItems.ELIAS_SPAWN_EGG.get());
                        output.accept(ModItems.SACRED_CULTIST_SPAWN_EGG.get());
                        output.accept(ModItems.TEMPLAR_SPAWN_EGG.get());
                        output.accept(ModItems.CINDER_SPAWN_EGG.get());
                        output.accept(ModItems.LAVA_LENS.get());
                        output.accept(ModItems.TATTERED_WINGS.get());
                        output.accept(ModItems.SERAPH_WINGS.get());
                        output.accept(ModItems.FALLEN_WINGS.get());
                        output.accept(ModItems.SACRED_PRIEST_SPAWN_EGG.get());
                        output.accept(ModItems.ANDRAS_SPAWN_EGG.get());
                        output.accept(ModItems.OSTARA_SPAWN_EGG.get());
                        output.accept(ModItems.BOOK_OF_CONTRACTS.get());
                        output.accept(ModItems.HOLLOW_RELIC.get());
                        output.accept(ModItems.MARK_OF_CAIN.get());
                        output.accept(ModItems.HELL_SEAL.get());
                        output.accept(ModItems.EDENS_THORN.get());
                        output.accept(ModItems.DEBT_COLLECTOR.get());
                        output.accept(ModItems.VIGILS_SORROW.get());
                        output.accept(ModItems.EVES_SECRET.get());
                        output.accept(ModItems.FLAME_OF_GOD.get());
                        output.accept(ModItems.PANDORA_BOX.get());
                        output.accept(ModItems.OBLIVION_KEY.get());
                        output.accept(ModItems.FOREST_KEY.get());
                        output.accept(ModItems.CHAMPION_SEEKER.get());
                        output.accept(ModItems.RAW_EDENWEED.get());
                        output.accept(ModItems.DRIED_EDENWEED.get());
                        output.accept(ModItems.MINOR_SIGHT_POTION.get());
                        output.accept(ModItems.BENCH.get());
                        output.accept(ModItems.DRYING_RACK_ITEM.get());
                        output.accept(ModItems.SACRED_ALTAR_ITEM.get());
                        output.accept(ModItems.CORRUPTED_OBSIDIAN_ITEM.get());
                        output.accept(ModItems.BLACK_STEM_ITEM.get());
                        output.accept(ModItems.BLACK_WART_BLOCK_ITEM.get());
                        output.accept(ModItems.BLACK_ROOTS_ITEM.get());
                        output.accept(ModItems.BLACK_FUNGUS_ITEM.get());
                        output.accept(ModItems.BLACK_GRASS_ITEM.get());
                        output.accept(ModItems.BLACK_SPROUTS_ITEM.get());
                        output.accept(ModItems.ASH_BLOCK_ITEM.get());
                        output.accept(ModItems.BLACK_VINES_ITEM.get());
                        output.accept(ModItems.OBLIVION_SHROOMLIGHT_ITEM.get());
                        output.accept(ModItems.SOUL_SPORE.get());
                        output.accept(ModItems.PETRIFIED_SKULL_ITEM.get());
                        output.accept(ModItems.WITHERED_OBSIDIAN_ITEM.get());
                        output.accept(ModItems.INFERNAL_ESSENCE.get());
                        output.accept(ModItems.COMPRESSED_NETHERRACK_ITEM.get());
                        output.accept(ModItems.COMPRESSED_NETHERRACK_STAIRS_ITEM.get());
                        output.accept(ModItems.COMPRESSED_NETHERRACK_SLAB_ITEM.get());
                        output.accept(ModItems.COMPRESSED_NETHERRACK_WALL_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_STAIRS_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_SLAB_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_WALL_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_BRICKS_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_BRICK_STAIRS_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_BRICK_SLAB_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_BRICK_WALL_ITEM.get());
                        output.accept(ModItems.POLISHED_BLIGHTSTONE_ITEM.get());
                        output.accept(ModItems.POLISHED_BLIGHTSTONE_STAIRS_ITEM.get());
                        output.accept(ModItems.POLISHED_BLIGHTSTONE_SLAB_ITEM.get());
                        output.accept(ModItems.POLISHED_BLIGHTSTONE_WALL_ITEM.get());
                        output.accept(ModItems.CHISELED_POLISHED_BLIGHTSTONE_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_SMALL_BRICKS_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_SMALL_BRICK_STAIRS_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_SMALL_BRICK_SLAB_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_SMALL_BRICK_WALL_ITEM.get());
                        output.accept(ModItems.CRACKED_BLIGHTSTONE_BRICKS_ITEM.get());
                        output.accept(ModItems.ARGANITE_ITEM.get());
                        output.accept(ModItems.ARGANITE_STAIRS_ITEM.get());
                        output.accept(ModItems.ARGANITE_SLAB_ITEM.get());
                        output.accept(ModItems.ARGANITE_WALL_ITEM.get());
                        output.accept(ModItems.ARGANITE_BRICKS_ITEM.get());
                        output.accept(ModItems.ARGANITE_BRICKS_STAIRS_ITEM.get());
                        output.accept(ModItems.ARGANITE_BRICKS_SLAB_ITEM.get());
                        output.accept(ModItems.ARGANITE_BRICKS_WALL_ITEM.get());
                        output.accept(ModItems.QUARTZITE_ITEM.get());
                        output.accept(ModItems.PORCELAIN_ITEM.get());
                        output.accept(ModItems.PORCELAIN_STAIRS_ITEM.get());
                        output.accept(ModItems.PORCELAIN_SLAB_ITEM.get());
                        output.accept(ModItems.POLISHED_PORCELAIN_ITEM.get());
                        output.accept(ModItems.POLISHED_PORCELAIN_STAIRS_ITEM.get());
                        output.accept(ModItems.POLISHED_PORCELAIN_SLAB_ITEM.get());
                        output.accept(ModItems.TILED_PORCELAIN_ITEM.get());
                        output.accept(ModItems.QUARTZ_CLUSTER_ITEM.get());
                        output.accept(ModItems.ARGANITE_DUST.get());
                        output.accept(ModItems.CALCITE_DUST.get());
                        output.accept(ModItems.QUARTZ_DUST.get());
                        output.accept(ModItems.PORCELAIN_DUST.get());
                        output.accept(ModItems.POLISHED_PORCELAIN_DUST.get());
                        output.accept(ModItems.TILED_PORCELAIN_DUST.get());
                        output.accept(ModItems.FLESH_BLOCK_ITEM.get());
                        output.accept(ModItems.MOUTH_BLOCK_ITEM.get());
                        output.accept(ModItems.HAUNTED_LOG_ITEM.get());
                        output.accept(ModItems.HAUNTED_LEAVES_ITEM.get());
                        output.accept(ModItems.HAUNTED_SHELF_SHROOM_ITEM.get());
                        output.accept(ModItems.CHARRED_FLESH_ITEM.get());
                        output.accept(ModItems.GRAVE_SAND_ITEM.get());
                        output.accept(ModItems.GRAVE_SANDSTONE_ITEM.get());
                        output.accept(ModItems.CRYPT_GLASS_ITEM.get());
                        output.accept(ModItems.HELLGLASS_ITEM.get());
                        output.accept(ModItems.CRYPT_GLASS_PANE_ITEM.get());
                        output.accept(ModItems.HELLGLASS_PANE_ITEM.get());
                        output.accept(ModItems.CHURCH_CANDELABRE_ITEM.get());
                        output.accept(ModItems.CHURCH_TOMBSTONE_ITEM.get());
                        output.accept(ModItems.WEEPING_EYE.get());
                        output.accept(ModItems.POROUS_FLESH_ITEM.get());
                        output.accept(ModItems.TENDONS_ITEM.get());
                        output.accept(ModItems.VISCERA_ITEM.get());
                        output.accept(ModItems.HAIR_ITEM.get());
                        output.accept(ModItems.SKIN_ITEM.get());
                        output.accept(ModItems.SPINE_ITEM.get());
                        output.accept(ModItems.NERVES_ITEM.get());
                        output.accept(ModItems.HEART_ITEM.get());
                        output.accept(ModItems.CAPILLARIES_ITEM.get());
                        output.accept(ModItems.ARTERIES_ITEM.get());
                        output.accept(ModItems.EYEBULB_ITEM.get());
                        output.accept(ModItems.SMALL_EYEBULB_ITEM.get());
                        output.accept(ModItems.FLESH_SLAB_ITEM.get());
                        output.accept(ModItems.FLESH_CARPET_ITEM.get());
                        output.accept(ModItems.ORNATED_CARPET_ITEM.get());
                        output.accept(ModItems.RED_CHAIN_ITEM.get());
                        output.accept(ModItems.BLACK_NYLIUM_ITEM.get());
                        output.accept(ModItems.BLIGHTSTONE_NYLIUM_ITEM.get());
                        output.accept(ModItems.TALL_CRIMSON_ROOTS_ITEM.get());
                        output.accept(ModItems.TALL_BLACK_GRASS_ITEM.get());
                        output.accept(ModItems.LUST_FLOWER_ITEM.get());
                        output.accept(ModItems.HELL_FORGE_CONTROLLER_ITEM.get());
                        output.accept(ModItems.DEMONIC_CORE_ITEM.get());
                        output.accept(ModItems.HELL_FORGE_VAULT_ITEM.get());
                        output.accept(ModItems.FORGE_WRENCH.get());
                        output.accept(ModItems.LYRIUM_BLOCK_ITEM.get());
                        output.accept(ModItems.LYRIUM_CLUSTER_ITEM.get());
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

                        // Circle 2: Ebony and the Dark Coven
                        output.accept(ModItems.EBONY_DEBRIS_ITEM.get());
                        output.accept(ModItems.EBONY_SCRAP.get());
                        output.accept(ModItems.EBONY_BAR.get());
                        output.accept(ModItems.EBONY_SWORD.get());
                        output.accept(ModItems.EBONY_AXE.get());
                        output.accept(ModItems.EBONY_SHOVEL.get());
                        output.accept(ModItems.EBONY_PICKAXE.get());
                        output.accept(ModItems.EBONY_HOE.get());
                        output.accept(ModItems.EBONY_HELMET.get());
                        output.accept(ModItems.EBONY_CHESTPLATE.get());
                        output.accept(ModItems.EBONY_LEGGINGS.get());
                        output.accept(ModItems.EBONY_BOOTS.get());
                        output.accept(ModItems.DARK_COVEN_I.get());
                        output.accept(ModItems.DARK_COVEN_II.get());
                        output.accept(ModItems.DARK_COVEN_III.get());
                        output.accept(ModItems.DARK_COVEN_IV.get());
                        output.accept(ModItems.DARK_COVEN_V.get());
                        output.accept(ModItems.DARK_COVEN_VI.get());
                        output.accept(ModItems.DARK_COVEN_VII.get());
                        output.accept(ModItems.CAIN_SPAWN_EGG.get());
                        output.accept(ModItems.FLESH_GOLEM_SPAWN_EGG.get());

                        // Enchanted books at max level - the only way to get one of these
                        // otherwise is the enchanting table/anvil RNG, or an /enchant command.
                        HolderLookup.RegistryLookup<Enchantment> enchantments = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT);
                        acceptEnchantedBook(output, enchantments, ModEnchantments.BANE_OF_HUMANITY, 5);
                        acceptEnchantedBook(output, enchantments, ModEnchantments.EVIL, 5);
                        acceptEnchantedBook(output, enchantments, ModEnchantments.REAPING, 3);
                        acceptEnchantedBook(output, enchantments, ModEnchantments.HOLY, 3);
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
