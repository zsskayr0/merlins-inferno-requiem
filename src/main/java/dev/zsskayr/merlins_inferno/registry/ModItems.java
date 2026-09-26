package dev.zsskayr.merlins_inferno.registry;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.GrimmoriumItem;
import dev.zsskayr.merlins_inferno.item.ModTiers;

/**
 * Central registry for every {@code Item} the mod adds (including {@code BlockItem}s,
 * which stay here rather than in {@link ModBlocks} since items are what the player
 * actually interacts with in an inventory).
 * <p>
 * Add new items as {@code public static final} fields below, then call
 * {@link #register(IEventBus)} once from the main mod class.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Merlins_inferno.MODID);

    // The mod's in-game guidebook - see item.GrimmoriumItem for what "using" it does
    // (data/merlins_inferno/patchouli_books/guide/) and compat.patchouli for why Patchouli being
    // absent doesn't break this item.
    public static final DeferredItem<GrimmoriumItem> GRIMMORIUM = ITEMS.registerItem("grimmorium", GrimmoriumItem::new);

    // --- Rowanwood material set: see ModTiers.ROWANWOOD / ModArmorMaterials.ROWANWOOD for the
    // balance rationale (mines like Diamond, hits/blocks like Iron, outlasts both). ---

    public static final DeferredItem<Item> ROWANWOOD_BAR = ITEMS.registerSimpleItem("rowanwood_bar", new Item.Properties());

    // How Rowanwood Bar is actually obtained: either essence, surrounded by log/leaves/iron in the
    // same shape (see data/merlins_inferno/recipe/rowanwood_scrap_from_*.json), crafts into a
    // Rowanwood Scrap - which then smelts (or blasts) into the bar itself. Plain items, no
    // in-world behavior of their own.
    // The three essences (design decision):
    //  - Mundane Essence: early game, obtained from the Druids (DruidEntity#updateTrades - it's
    //    what they hand over, not a currency the player pays them with).
    //  - Fae Essence: dropped by the Dullahan (not implemented yet); see below.
    //  - Mundane and Fae both craft the Rowanwood Scrap (data/.../recipe/rowanwood_scrap_from_*.json).
    //  - Otherworld Essence: something far more mystical, reserved for mid game and beyond - kept
    //    registered but deliberately has no source or recipe yet.
    public static final DeferredItem<Item> OTHERWORLD_ESSENCE = ITEMS.registerSimpleItem("otherworld_essence", new Item.Properties());
    public static final DeferredItem<Item> MUNDANE_ESSENCE = ITEMS.registerSimpleItem("mundane_essence", new Item.Properties());
    // Dropped by third-party "fairy" mobs at 10% - see ModTags.EntityTypes.FAIRIES and
    // data/merlins_inferno/loot_modifiers/fae_essence_from_fairies.json (empty tag by default,
    // since this mod doesn't know which fairy-adding mods are installed).
    public static final DeferredItem<Item> FAE_ESSENCE = ITEMS.registerSimpleItem("fae_essence", new Item.Properties());
    public static final DeferredItem<Item> ROWANWOOD_SCRAP = ITEMS.registerSimpleItem("rowanwood_scrap", new Item.Properties());

    public static final DeferredItem<SwordItem> ROWANWOOD_SWORD = ITEMS.registerItem("rowanwood_sword",
            props -> new SwordItem(ModTiers.ROWANWOOD, props.attributes(SwordItem.createAttributes(ModTiers.ROWANWOOD, 3, -2.4F))));
    public static final DeferredItem<AxeItem> ROWANWOOD_AXE = ITEMS.registerItem("rowanwood_axe",
            props -> new AxeItem(ModTiers.ROWANWOOD, props.attributes(AxeItem.createAttributes(ModTiers.ROWANWOOD, 6.0F, -3.1F))));
    public static final DeferredItem<ShovelItem> ROWANWOOD_SHOVEL = ITEMS.registerItem("rowanwood_shovel",
            props -> new ShovelItem(ModTiers.ROWANWOOD, props.attributes(ShovelItem.createAttributes(ModTiers.ROWANWOOD, 1.5F, -3.0F))));
    public static final DeferredItem<PickaxeItem> ROWANWOOD_PICKAXE = ITEMS.registerItem("rowanwood_pickaxe",
            props -> new PickaxeItem(ModTiers.ROWANWOOD, props.attributes(PickaxeItem.createAttributes(ModTiers.ROWANWOOD, 1.0F, -2.8F))));
    public static final DeferredItem<HoeItem> ROWANWOOD_HOE = ITEMS.registerItem("rowanwood_hoe",
            props -> new HoeItem(ModTiers.ROWANWOOD, props.attributes(HoeItem.createAttributes(ModTiers.ROWANWOOD, -2.0F, -1.0F))));

    public static final DeferredItem<ArmorItem> ROWANWOOD_HELMET = ITEMS.registerItem("rowanwood_helmet",
            props -> new ArmorItem(ModArmorMaterials.ROWANWOOD, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(40))));
    public static final DeferredItem<ArmorItem> ROWANWOOD_CHESTPLATE = ITEMS.registerItem("rowanwood_chestplate",
            props -> new ArmorItem(ModArmorMaterials.ROWANWOOD, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(40))));
    public static final DeferredItem<ArmorItem> ROWANWOOD_LEGGINGS = ITEMS.registerItem("rowanwood_leggings",
            props -> new ArmorItem(ModArmorMaterials.ROWANWOOD, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(40))));
    public static final DeferredItem<ArmorItem> ROWANWOOD_BOOTS = ITEMS.registerItem("rowanwood_boots",
            props -> new ArmorItem(ModArmorMaterials.ROWANWOOD, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(40))));

    // --- Demonblood material set: see ModTiers.DEMONBLOOD for the balance rationale (Netherite's
    // stats, higher enchantability, +10% damage vs "humans" via DemonbloodCombatHandler - called
    // out below in each tool's tooltip). Tool attack attributes mirror vanilla's actual Netherite
    // tool values 1:1. Every demonblood_*.json recipe also crafts its result pre-enchanted with
    // Bane of Humanity I (see data/merlins_inferno/recipe/demonblood_*.json's result.components). ---

    // Dropped by Imps (see data/merlins_inferno/loot_table/entities/imp.json). Combined with
    // Infernal Essence + Netherite Scrap into Demonblood Scrap
    // (data/merlins_inferno/recipe/demonblood_scrap.json), which the Hell Forge then turns into the
    // bar below (data/merlins_inferno/recipe/demonblood_bar_from_hell_forge.json).
    public static final DeferredItem<Item> DEMON_BLOOD = ITEMS.registerSimpleItem("demon_blood", new Item.Properties().fireResistant());
    public static final DeferredItem<Item> INFERNAL_SINEW = ITEMS.registerSimpleItem("infernal_sinew", new Item.Properties().fireResistant());
    public static final DeferredItem<Item> WITHERED_BONE = ITEMS.registerSimpleItem("withered_bone", new Item.Properties().fireResistant());
    public static final DeferredItem<Item> DEMONBLOOD_SCRAP = ITEMS.registerSimpleItem("demonblood_scrap", new Item.Properties().fireResistant());

    // Plain crafting material - no in-world use yet beyond crafting the tools below.
    public static final DeferredItem<Item> DEMONBLOOD_BAR = ITEMS.registerSimpleItem("demonblood_bar", new Item.Properties().fireResistant());

    public static final DeferredItem<SwordItem> DEMONBLOOD_SWORD = ITEMS.registerItem("demonblood_sword",
            props -> new SwordItem(ModTiers.DEMONBLOOD, props.fireResistant().attributes(SwordItem.createAttributes(ModTiers.DEMONBLOOD, 3, -2.4F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });
    public static final DeferredItem<AxeItem> DEMONBLOOD_AXE = ITEMS.registerItem("demonblood_axe",
            props -> new AxeItem(ModTiers.DEMONBLOOD, props.fireResistant().attributes(AxeItem.createAttributes(ModTiers.DEMONBLOOD, 5.0F, -3.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });
    public static final DeferredItem<ShovelItem> DEMONBLOOD_SHOVEL = ITEMS.registerItem("demonblood_shovel",
            props -> new ShovelItem(ModTiers.DEMONBLOOD, props.fireResistant().attributes(ShovelItem.createAttributes(ModTiers.DEMONBLOOD, 1.5F, -3.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });
    public static final DeferredItem<PickaxeItem> DEMONBLOOD_PICKAXE = ITEMS.registerItem("demonblood_pickaxe",
            props -> new PickaxeItem(ModTiers.DEMONBLOOD, props.fireResistant().attributes(PickaxeItem.createAttributes(ModTiers.DEMONBLOOD, 1.0F, -2.8F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });
    public static final DeferredItem<HoeItem> DEMONBLOOD_HOE = ITEMS.registerItem("demonblood_hoe",
            props -> new HoeItem(ModTiers.DEMONBLOOD, props.fireResistant().attributes(HoeItem.createAttributes(ModTiers.DEMONBLOOD, -4.0F, 0.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });

    public static final DeferredItem<ArmorItem> DEMONBLOOD_HELMET = ITEMS.registerItem("demonblood_helmet",
            props -> new ArmorItem(ModArmorMaterials.DEMONBLOOD, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(37)).fireResistant()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendDemonFuryTooltip(tooltip);
                }
            });
    public static final DeferredItem<ArmorItem> DEMONBLOOD_CHESTPLATE = ITEMS.registerItem("demonblood_chestplate",
            props -> new ArmorItem(ModArmorMaterials.DEMONBLOOD, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(37)).fireResistant()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendDemonFuryTooltip(tooltip);
                }
            });
    public static final DeferredItem<ArmorItem> DEMONBLOOD_LEGGINGS = ITEMS.registerItem("demonblood_leggings",
            props -> new ArmorItem(ModArmorMaterials.DEMONBLOOD, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(37)).fireResistant()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendDemonFuryTooltip(tooltip);
                }
            });
    public static final DeferredItem<ArmorItem> DEMONBLOOD_BOOTS = ITEMS.registerItem("demonblood_boots",
            props -> new ArmorItem(ModArmorMaterials.DEMONBLOOD, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(37)).fireResistant()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendDemonFuryTooltip(tooltip);
                }
            });

    /** Shared tooltip line for every Demonblood tool - see {@code DemonbloodCombatHandler}. */
    private static void appendHumanDamageTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("item.merlins_inferno.demonblood_tooltip.humans_bonus").withStyle(ChatFormatting.DARK_RED));
    }

    /** Shared tooltip line for every Demonblood armor piece - see {@code DemonbloodCombatHandler}. */
    private static void appendDemonFuryTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("item.merlins_inferno.demonblood_armor_tooltip.demon_fury").withStyle(ChatFormatting.DARK_RED));
    }

    // --- Lyrium/Seraphium material set: see ModTiers.SERAPHIUM / ModArmorMaterials.SERAPHIUM for
    // the balance rationale (the strongest of the 3 sets). Lyrium's refinement chain (design doc:
    // "Pilar Angelical") has 6 conceptual stages now (Shard added below the original 5, per the
    // texture set actually provided); only the first 4 have any obtainment/crafting path wired up
    // yet - Pure and True are registered as plain hooks for the altar/structure content that
    // unlocks them, deliberately out of scope for now. ---

    // Stage 0: the literal ore fragment. Refined Lyrium can be split into three shards, which are
    // combined with an iron nugget to make the Lyrium Rod used by Seraphium tools.
    public static final DeferredItem<Item> LYRIUM_SHARD = ITEMS.registerSimpleItem("lyrium_shard", new Item.Properties());
    public static final DeferredItem<Item> LYRIUM_ROD = ITEMS.registerSimpleItem("lyrium_rod", new Item.Properties());
    // Stage 1: mined raw (no ore-block/worldgen wired up yet - obtain via /give or drops added
    // later, same situation Infernal Essence started in). Its own "weapon" role isn't a separate
    // item - holding it already triggers Sanctified (see SanctifiedTickHandler) at a level that
    // includes the Undead damage bonus, which is the whole point of carrying it into a fight.
    public static final DeferredItem<Item> LYRIUM_RAW = ITEMS.registerSimpleItem("lyrium_raw", new Item.Properties());
    // Stage 2: smelting raw Lyrium (the "quick and dirty" process, early-game accessible) gives
    // this - safe to carry/store (does not trigger Sanctified) and usable in basic potions later.
    public static final DeferredItem<Item> LYRIUM_IMPURE = ITEMS.registerSimpleItem("lyrium_impure", new Item.Properties());
    // Stage 3: the first genuinely useful stage - unlocks the Seraphium Bar recipe. Obtained via
    // the Cleric villager trade (LyriumVillagerTrades) or rare pillager-structure loot
    // (see data/merlins_inferno/loot_modifiers) - no crafting path yet (the brief leaves the
    // mid-game processing structure/machine as a later decision).
    public static final DeferredItem<Item> LYRIUM_REFINED = ITEMS.registerSimpleItem("lyrium_refined", new Item.Properties());
    // Stages 4-5: mid/end-game, explicitly out of scope beyond existing as registered items (hooks
    // for future altar/structure content).
    public static final DeferredItem<Item> LYRIUM_PURE = ITEMS.registerSimpleItem("lyrium_pure", new Item.Properties());
    public static final DeferredItem<Item> LYRIUM_TRUE = ITEMS.registerSimpleItem("lyrium_true", new Item.Properties());

    // Obtainment method is a deliberate TODO per the design doc - registered now so the Seraphium
    // Scrap recipe isn't blocked on it.
    public static final DeferredItem<Item> CELESTIAL_ESSENCE = ITEMS.registerSimpleItem("celestial_essence", new Item.Properties());

    // Mirrors Rowanwood's own 2-stage chain: refined Lyrium + Diamond + Celestial Essence crafts
    // into this Scrap, which then smelts into the actual Bar (see
    // data/merlins_inferno/recipe/seraphium_scrap.json / seraphium_bar_from_smelting.json).
    public static final DeferredItem<Item> SERAPHIUM_SCRAP = ITEMS.registerSimpleItem("seraphium_scrap", new Item.Properties());
    public static final DeferredItem<Item> SERAPHIUM_BAR = ITEMS.registerSimpleItem("seraphium_bar", new Item.Properties());

    public static final DeferredItem<SwordItem> SERAPHIUM_SWORD = ITEMS.registerItem("seraphium_sword",
            props -> new SwordItem(ModTiers.SERAPHIUM, props.attributes(SwordItem.createAttributes(ModTiers.SERAPHIUM, 3, -2.4F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendSeraphiumTooltip(tooltip);
                }
            });
    public static final DeferredItem<AxeItem> SERAPHIUM_AXE = ITEMS.registerItem("seraphium_axe",
            props -> new AxeItem(ModTiers.SERAPHIUM, props.attributes(AxeItem.createAttributes(ModTiers.SERAPHIUM, 6.0F, -3.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendSeraphiumTooltip(tooltip);
                }
            });
    public static final DeferredItem<ShovelItem> SERAPHIUM_SHOVEL = ITEMS.registerItem("seraphium_shovel",
            props -> new ShovelItem(ModTiers.SERAPHIUM, props.attributes(ShovelItem.createAttributes(ModTiers.SERAPHIUM, 1.5F, -3.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendSeraphiumTooltip(tooltip);
                }
            });
    public static final DeferredItem<PickaxeItem> SERAPHIUM_PICKAXE = ITEMS.registerItem("seraphium_pickaxe",
            props -> new PickaxeItem(ModTiers.SERAPHIUM, props.attributes(PickaxeItem.createAttributes(ModTiers.SERAPHIUM, 1.0F, -2.8F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendSeraphiumTooltip(tooltip);
                }
            });
    public static final DeferredItem<HoeItem> SERAPHIUM_HOE = ITEMS.registerItem("seraphium_hoe",
            props -> new HoeItem(ModTiers.SERAPHIUM, props.attributes(HoeItem.createAttributes(ModTiers.SERAPHIUM, -5.0F, 0.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendSeraphiumTooltip(tooltip);
                }
            });

    /**
     * Shared tooltip line for every Seraphium tool - Deuteronomy 13:5 (Vulgate), the refrain used
     * throughout the book for casting out evil: "sic auferes malum de medio tui" ("so shall you
     * purge the evil from your midst"). See {@code event.SeraphiumCombatHandler} for what the set
     * actually does in combat.
     */
    private static void appendSeraphiumTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("item.merlins_inferno.seraphium_tooltip.latin_verse").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }

    public static final DeferredItem<ArmorItem> SERAPHIUM_HELMET = ITEMS.registerItem("seraphium_helmet",
            props -> new ArmorItem(ModArmorMaterials.SERAPHIUM, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(45))));
    public static final DeferredItem<ArmorItem> SERAPHIUM_CHESTPLATE = ITEMS.registerItem("seraphium_chestplate",
            props -> new ArmorItem(ModArmorMaterials.SERAPHIUM, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(45))));
    public static final DeferredItem<ArmorItem> SERAPHIUM_LEGGINGS = ITEMS.registerItem("seraphium_leggings",
            props -> new ArmorItem(ModArmorMaterials.SERAPHIUM, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(45))));
    public static final DeferredItem<ArmorItem> SERAPHIUM_BOOTS = ITEMS.registerItem("seraphium_boots",
            props -> new ArmorItem(ModArmorMaterials.SERAPHIUM, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(45))));

    // --- BlockItems for the Hallowed Grove's two trees (see ModBlocks). ---

    public static final DeferredItem<BlockItem> ASHWOOD_LOG_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_LOG);
    public static final DeferredItem<BlockItem> ASHWOOD_WOOD_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_WOOD);
    public static final DeferredItem<BlockItem> ASHWOOD_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_PLANKS);
    public static final DeferredItem<BlockItem> ASHWOOD_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_LEAVES);
    public static final DeferredItem<BlockItem> ASHWOOD_SAPLING_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_SAPLING);
    public static final DeferredItem<BlockItem> ASHWOOD_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_STAIRS);
    public static final DeferredItem<BlockItem> ASHWOOD_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_SLAB);
    public static final DeferredItem<BlockItem> ASHWOOD_FENCE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_FENCE);

    public static final DeferredItem<BlockItem> STRIPPED_ASHWOOD_LOG_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STRIPPED_ASHWOOD_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_ASHWOOD_WOOD_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STRIPPED_ASHWOOD_WOOD);
    public static final DeferredItem<BlockItem> ASHWOOD_FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_FENCE_GATE);
    public static final DeferredItem<BlockItem> ASHWOOD_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_DOOR);
    public static final DeferredItem<BlockItem> ASHWOOD_TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_TRAPDOOR);
    public static final DeferredItem<BlockItem> ASHWOOD_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> ASHWOOD_BUTTON_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_BUTTON);
    // Signs place either the standing or the wall block depending on the clicked face, and stack to 16 like vanilla's.
    public static final DeferredItem<SignItem> ASHWOOD_SIGN_ITEM = ITEMS.registerItem("ashwood_sign",
            props -> new SignItem(props.stacksTo(16), ModBlocks.ASHWOOD_SIGN.get(), ModBlocks.ASHWOOD_WALL_SIGN.get()));
    public static final DeferredItem<HangingSignItem> ASHWOOD_HANGING_SIGN_ITEM = ITEMS.registerItem("ashwood_hanging_sign",
            props -> new HangingSignItem(ModBlocks.ASHWOOD_HANGING_SIGN.get(), ModBlocks.ASHWOOD_WALL_HANGING_SIGN.get(), props.stacksTo(16)));

    public static final DeferredItem<BlockItem> ROWANWOOD_LOG_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ROWANWOOD_LOG);
    public static final DeferredItem<BlockItem> ROWANWOOD_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ROWANWOOD_LEAVES);

    // --- Hell Forge ingredients + the block itself (see ModBlocks). ---

    // Dropped by Nether mobs at a modest rarity - see data/merlins_inferno/loot_modifiers.
    public static final DeferredItem<Item> INFERNAL_ESSENCE = ITEMS.registerSimpleItem("infernal_essence", new Item.Properties());
    public static final DeferredItem<BlockItem> COMPRESSED_NETHERRACK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COMPRESSED_NETHERRACK);
    public static final DeferredItem<BlockItem> HELL_FORGE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HELL_FORGE);

    // Testing/admin convenience - the Druid otherwise only ever appears via natural spawning in
    // the Hallowed Grove (see ModBiomeProvider). Background/highlight colors: mossy forest green
    // with a warm bark-gold spot pattern, matching the "nature guardian" theme.
    public static final DeferredItem<DeferredSpawnEggItem> IMP_SPAWN_EGG = ITEMS.registerItem("imp_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.IMP, 0xA8231B, 0xF2C230, props));
    public static final DeferredItem<DeferredSpawnEggItem> DRUID_SPAWN_EGG = ITEMS.registerItem("druid_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.DRUID, 0x4A6B3D, 0xC9A66B, props));

    // --- Debug-only tooling. No recipe, never added to the creative tab (see
    // ModCreativeModeTabs) - /give merlins_inferno:debug_cursed-nullifier is the only way to get
    // one. Not meant to represent a "real" item tier: Tiers.WOOD has a +0 attack damage bonus, so
    // a base damage of 999 lands the ATTACK_DAMAGE attribute at exactly 999, which combined with
    // every entity's own base 1.0 unarmed damage shows as 1000 in-game. Unbreakable so it never
    // needs repairing mid-test. ---
    public static final DeferredItem<SwordItem> DEBUG_CURSED_NULLIFIER = ITEMS.registerItem("debug_cursed-nullifier",
            props -> new SwordItem(Tiers.WOOD, props
                    .attributes(SwordItem.createAttributes(Tiers.WOOD, 999, -2.4F))
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
