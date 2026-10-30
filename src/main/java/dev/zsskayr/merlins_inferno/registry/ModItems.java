package dev.zsskayr.merlins_inferno.registry;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Rarity;
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
import dev.zsskayr.merlins_inferno.item.AntidoteItem;
import dev.zsskayr.merlins_inferno.item.ChampionSeekerItem;
import dev.zsskayr.merlins_inferno.item.DarkCovenItem;
import dev.zsskayr.merlins_inferno.item.MinorSightPotionItem;
import dev.zsskayr.merlins_inferno.item.GrimmoriumItem;
import dev.zsskayr.merlins_inferno.item.OblivionKeyItem;
import dev.zsskayr.merlins_inferno.item.PandoraBoxItem;
import dev.zsskayr.merlins_inferno.item.PortalKeyItem;
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
    public static final DeferredItem<dev.zsskayr.merlins_inferno.item.LavaLensItem> LAVA_LENS = ITEMS.registerItem("lava_lens",
            dev.zsskayr.merlins_inferno.item.LavaLensItem::new);

    // Elytra-style wings boosted by a keybind instead of fireworks (item.WingsItem; what sets each pair apart is its WingsProfile).
    // Tattered Wings belong to the mundane (Druidic) route and cannot be mended; the Pandora Box turns them into the Seraph
    // Wings (Angelical) or the Fallen Wings (Infernal). Durability is the elytra's 432, twice and three times that.
    public static final DeferredItem<dev.zsskayr.merlins_inferno.item.WingsItem> TATTERED_WINGS = ITEMS.registerItem("tattered_wings",
            props -> new dev.zsskayr.merlins_inferno.item.WingsItem(props.rarity(Rarity.RARE).durability(432),
                    new dev.zsskayr.merlins_inferno.item.WingsProfile(30 * 20, net.minecraft.core.particles.ParticleTypes.CLOUD,
                            wingsTexture("tattered_wings"),
                            new dev.zsskayr.merlins_inferno.item.WingsProfile.PerWorld(1, 1, 2),
                            dev.zsskayr.merlins_inferno.item.WingsProfile.PerWorld.FLAT,
                            ModTags.Items.MUNDANE_WINGS_ARMOR, null)));
    public static final DeferredItem<dev.zsskayr.merlins_inferno.item.WingsItem> SERAPH_WINGS = ITEMS.registerItem("seraph_wings",
            props -> new dev.zsskayr.merlins_inferno.item.WingsItem(props.rarity(Rarity.EPIC).durability(432 * 2),
                    new dev.zsskayr.merlins_inferno.item.WingsProfile(12 * 20, net.minecraft.core.particles.ParticleTypes.END_ROD,
                            wingsTexture("seraph_wings"),
                            new dev.zsskayr.merlins_inferno.item.WingsProfile.PerWorld(1, 2, 2),
                            new dev.zsskayr.merlins_inferno.item.WingsProfile.PerWorld(1, 0.75, 0.75),
                            ModTags.Items.SERAPH_WINGS_ARMOR, ModItems.SERAPHIUM_BAR)));
    // The Fallen Wings also count as an iron chestplate's worth of armour while worn in the chest slot.
    public static final DeferredItem<dev.zsskayr.merlins_inferno.item.WingsItem> FALLEN_WINGS = ITEMS.registerItem("fallen_wings",
            props -> new dev.zsskayr.merlins_inferno.item.WingsItem(props.rarity(Rarity.EPIC).fireResistant().durability(432 * 3)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder().add(
                            net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                            new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "fallen_wings_armor"), 6.0,
                                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                            net.minecraft.world.entity.EquipmentSlotGroup.CHEST).build()),
                    new dev.zsskayr.merlins_inferno.item.WingsProfile(16 * 20, net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            wingsTexture("fallen_wings"),
                            new dev.zsskayr.merlins_inferno.item.WingsProfile.PerWorld(1.5, 0.5, 0.5),
                            new dev.zsskayr.merlins_inferno.item.WingsProfile.PerWorld(1, 1.25, 1.25),
                            new dev.zsskayr.merlins_inferno.item.WingsProfile.PerWorld(1, 1, 0.5),
                            ModTags.Items.FALLEN_WINGS_ARMOR, ModItems.EBONY_BAR)));

    private static net.minecraft.resources.ResourceLocation wingsTexture(String name) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/wings/" + name + ".png");
    }

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
    //  - Fae Essence: dropped by the Dullahan.
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
    public static final DeferredItem<Item> MONSTER_LEATHER = ITEMS.registerSimpleItem("monster_leather", new Item.Properties());
    public static final DeferredItem<Item> DEVIL_FLESH = ITEMS.registerSimpleItem("devil_flesh", new Item.Properties().fireResistant());
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

    // Dropped by Elias (see loot_table/entities/elias.json); the Seraphium Scrap recipe needs it.
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
    public static final DeferredItem<BlockItem> GOTHIC_GREAT_GATE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GOTHIC_GREAT_GATE);
    public static final DeferredItem<BlockItem> ASHWOOD_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_GREAT_DOOR);
    public static final DeferredItem<BlockItem> SPRUCE_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SPRUCE_GREAT_DOOR);
    public static final DeferredItem<BlockItem> OAK_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.OAK_GREAT_DOOR);
    public static final DeferredItem<BlockItem> BIRCH_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BIRCH_GREAT_DOOR);
    public static final DeferredItem<BlockItem> JUNGLE_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.JUNGLE_GREAT_DOOR);
    public static final DeferredItem<BlockItem> ACACIA_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ACACIA_GREAT_DOOR);
    public static final DeferredItem<BlockItem> DARK_OAK_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DARK_OAK_GREAT_DOOR);
    public static final DeferredItem<BlockItem> MANGROVE_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MANGROVE_GREAT_DOOR);
    public static final DeferredItem<BlockItem> CHERRY_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CHERRY_GREAT_DOOR);
    public static final DeferredItem<BlockItem> BAMBOO_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BAMBOO_GREAT_DOOR);
    public static final DeferredItem<BlockItem> CRIMSON_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CRIMSON_GREAT_DOOR);
    public static final DeferredItem<BlockItem> WARPED_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.WARPED_GREAT_DOOR);
    public static final DeferredItem<BlockItem> HAUNTED_GREAT_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_GREAT_DOOR);
    public static final DeferredItem<BlockItem> ASHWOOD_TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_TRAPDOOR);
    public static final DeferredItem<BlockItem> ASHWOOD_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> ASHWOOD_BUTTON_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASHWOOD_BUTTON);
    // Signs place either the standing or the wall block depending on the clicked face, and stack to 16 like vanilla's.
    public static final DeferredItem<SignItem> ASHWOOD_SIGN_ITEM = ITEMS.registerItem("ashwood_sign",
            props -> new SignItem(props.stacksTo(16), ModBlocks.ASHWOOD_SIGN.get(), ModBlocks.ASHWOOD_WALL_SIGN.get()));
    public static final DeferredItem<HangingSignItem> ASHWOOD_HANGING_SIGN_ITEM = ITEMS.registerItem("ashwood_hanging_sign",
            props -> new HangingSignItem(ModBlocks.ASHWOOD_HANGING_SIGN.get(), ModBlocks.ASHWOOD_WALL_HANGING_SIGN.get(), props.stacksTo(16)));

    // Haunted wood set (see ModBlocks); HAUNTED_LOG_ITEM / HAUNTED_LEAVES_ITEM are defined with the Carnal Heap blocks.
    public static final DeferredItem<BlockItem> STRIPPED_HAUNTED_LOG_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STRIPPED_HAUNTED_LOG, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_WOOD_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_WOOD, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> STRIPPED_HAUNTED_WOOD_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STRIPPED_HAUNTED_WOOD, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_PLANKS, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_STAIRS, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_SLAB, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_FENCE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_FENCE, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_FENCE_GATE, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_DOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_DOOR, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_TRAPDOOR, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_PRESSURE_PLATE, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_BUTTON_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_BUTTON, new Item.Properties().fireResistant());
    public static final DeferredItem<SignItem> HAUNTED_SIGN_ITEM = ITEMS.registerItem("haunted_sign",
            props -> new SignItem(props.stacksTo(16).fireResistant(), ModBlocks.HAUNTED_SIGN.get(), ModBlocks.HAUNTED_WALL_SIGN.get()));
    public static final DeferredItem<HangingSignItem> HAUNTED_HANGING_SIGN_ITEM = ITEMS.registerItem("haunted_hanging_sign",
            props -> new HangingSignItem(ModBlocks.HAUNTED_HANGING_SIGN.get(), ModBlocks.HAUNTED_WALL_HANGING_SIGN.get(), props.stacksTo(16).fireResistant()));

    // The Haunted boat sails on lava as well as water and never burns.
    public static final DeferredItem<Item> HAUNTED_BOAT = ITEMS.registerItem("haunted_boat",
            props -> new dev.zsskayr.merlins_inferno.item.HauntedBoatItem(props.stacksTo(1).fireResistant()));

    public static final DeferredItem<BlockItem> ROWANWOOD_LOG_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ROWANWOOD_LOG);
    public static final DeferredItem<BlockItem> ROWANWOOD_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ROWANWOOD_LEAVES);

    // --- Lyrium geode blocks (see ModBlocks). The cluster drops Raw Lyrium. ---
    public static final DeferredItem<BlockItem> LYRIUM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.LYRIUM_BLOCK);
    public static final DeferredItem<BlockItem> LYRIUM_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.LYRIUM_CLUSTER);

    // --- Sacred Church blocks (creative/admin only in practice: the altar is unbreakable). ---
    public static final DeferredItem<BlockItem> SACRED_ALTAR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SACRED_ALTAR);
    public static final DeferredItem<BlockItem> CORRUPTED_OBSIDIAN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CORRUPTED_OBSIDIAN);
    public static final DeferredItem<BlockItem> PETRIFIED_SKULL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PETRIFIED_SKULL);
    public static final DeferredItem<BlockItem> WITHERED_OBSIDIAN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.WITHERED_OBSIDIAN);
    public static final DeferredItem<BlockItem> PURIFIED_OBSIDIAN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PURIFIED_OBSIDIAN);

    // --- Hell Forge ingredients + the block itself (see ModBlocks). ---

    // Dropped by Nether mobs at a modest rarity - see data/merlins_inferno/loot_modifiers.
    public static final DeferredItem<Item> INFERNAL_ESSENCE = ITEMS.registerSimpleItem("infernal_essence", new Item.Properties());
    public static final DeferredItem<BlockItem> COMPRESSED_NETHERRACK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COMPRESSED_NETHERRACK);
    public static final DeferredItem<BlockItem> COMPRESSED_NETHERRACK_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COMPRESSED_NETHERRACK_STAIRS);
    public static final DeferredItem<BlockItem> COMPRESSED_NETHERRACK_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COMPRESSED_NETHERRACK_SLAB);
    public static final DeferredItem<BlockItem> COMPRESSED_NETHERRACK_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COMPRESSED_NETHERRACK_WALL);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_STAIRS);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_SLAB);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_WALL);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_BRICKS);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_BRICK_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_BRICK_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_BRICK_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> POLISHED_BLIGHTSTONE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_BLIGHTSTONE);
    public static final DeferredItem<BlockItem> POLISHED_BLIGHTSTONE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_BLIGHTSTONE_STAIRS);
    public static final DeferredItem<BlockItem> POLISHED_BLIGHTSTONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_BLIGHTSTONE_SLAB);
    public static final DeferredItem<BlockItem> POLISHED_BLIGHTSTONE_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_BLIGHTSTONE_WALL);
    public static final DeferredItem<BlockItem> CHISELED_POLISHED_BLIGHTSTONE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CHISELED_POLISHED_BLIGHTSTONE);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_SMALL_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_SMALL_BRICKS);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_SMALL_BRICK_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_SMALL_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_SMALL_BRICK_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_SMALL_BRICK_SLAB);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_SMALL_BRICK_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_SMALL_BRICK_WALL);
    public static final DeferredItem<BlockItem> CRACKED_BLIGHTSTONE_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CRACKED_BLIGHTSTONE_BRICKS);
    public static final DeferredItem<BlockItem> ARGANITE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARGANITE);
    public static final DeferredItem<BlockItem> ARGANITE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARGANITE_STAIRS);
    public static final DeferredItem<BlockItem> ARGANITE_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARGANITE_SLAB);
    public static final DeferredItem<BlockItem> ARGANITE_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARGANITE_WALL);
    public static final DeferredItem<BlockItem> ARGANITE_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARGANITE_BRICKS);
    public static final DeferredItem<BlockItem> ARGANITE_BRICKS_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARGANITE_BRICKS_STAIRS);
    public static final DeferredItem<BlockItem> ARGANITE_BRICKS_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARGANITE_BRICKS_SLAB);
    public static final DeferredItem<BlockItem> ARGANITE_BRICKS_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARGANITE_BRICKS_WALL);
    public static final DeferredItem<BlockItem> QUARTZITE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.QUARTZITE);
    public static final DeferredItem<BlockItem> PORCELAIN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PORCELAIN);
    public static final DeferredItem<BlockItem> PORCELAIN_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PORCELAIN_STAIRS);
    public static final DeferredItem<BlockItem> PORCELAIN_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PORCELAIN_SLAB);
    public static final DeferredItem<BlockItem> POLISHED_PORCELAIN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_PORCELAIN);
    public static final DeferredItem<BlockItem> POLISHED_PORCELAIN_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_PORCELAIN_STAIRS);
    public static final DeferredItem<BlockItem> POLISHED_PORCELAIN_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.POLISHED_PORCELAIN_SLAB);
    public static final DeferredItem<BlockItem> TILED_PORCELAIN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TILED_PORCELAIN);
    public static final DeferredItem<BlockItem> QUARTZ_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.QUARTZ_CLUSTER);
    public static final DeferredItem<Item> ARGANITE_DUST = ITEMS.registerSimpleItem("arganite_dust", new Item.Properties());
    public static final DeferredItem<Item> CALCITE_DUST = ITEMS.registerSimpleItem("calcite_dust", new Item.Properties());
    public static final DeferredItem<Item> QUARTZ_DUST = ITEMS.registerSimpleItem("quartz_dust", new Item.Properties());
    public static final DeferredItem<Item> PORCELAIN_DUST = ITEMS.registerSimpleItem("porcelain_dust", new Item.Properties());
    public static final DeferredItem<Item> POLISHED_PORCELAIN_DUST = ITEMS.registerSimpleItem("polished_porcelain_dust", new Item.Properties());
    public static final DeferredItem<Item> TILED_PORCELAIN_DUST = ITEMS.registerSimpleItem("tiled_porcelain_dust", new Item.Properties());
    public static final DeferredItem<BlockItem> MOUTH_BLOCK_ITEM =ITEMS.registerSimpleBlockItem(ModBlocks.MOUTH_BLOCK);
    public static final DeferredItem<BlockItem> FLESH_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.FLESH_BLOCK);
    public static final DeferredItem<BlockItem> HAUNTED_LOG_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_LOG, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> HAUNTED_SHELF_SHROOM_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_SHELF_SHROOM);
    public static final DeferredItem<BlockItem> HAUNTED_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_LEAVES, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> GRAVE_SAND_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVE_SAND);
    public static final DeferredItem<BlockItem> GRAVE_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVE_SANDSTONE);
    public static final DeferredItem<BlockItem> CRYPT_GLASS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CRYPT_GLASS);
    public static final DeferredItem<BlockItem> HELLGLASS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HELLGLASS, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> CRYPT_GLASS_PANE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CRYPT_GLASS_PANE);
    public static final DeferredItem<BlockItem> GOTHIC_IRON_BARS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GOTHIC_IRON_BARS);
    public static final DeferredItem<BlockItem> GOTHIC_GATE_ITEM = ITEMS.register("gothic_gate",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.GOTHIC_GATE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HELLGLASS_PANE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HELLGLASS_PANE, new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> CHURCH_CANDELABRE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CHURCH_CANDELABRE);
    public static final DeferredItem<BlockItem> CHURCH_TOMBSTONE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CHURCH_TOMBSTONE);
    // Rare result of feeding the Mouth Block (see MouthBlock); the heart of the Hell Sight.
    public static final DeferredItem<Item> WEEPING_EYE = ITEMS.registerSimpleItem("weeping_eye", new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredItem<BlockItem> CHARRED_FLESH_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CHARRED_FLESH);
    public static final DeferredItem<BlockItem> POROUS_FLESH_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.POROUS_FLESH);
    public static final DeferredItem<BlockItem> VISCERA_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.VISCERA);
    public static final DeferredItem<BlockItem> TENDONS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TENDONS);
    public static final DeferredItem<BlockItem> HAIR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HAIR);
    public static final DeferredItem<BlockItem> SMALL_EYEBULB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SMALL_EYEBULB);
    public static final DeferredItem<BlockItem> SKIN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SKIN);
    public static final DeferredItem<BlockItem> SPINE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SPINE);
    public static final DeferredItem<BlockItem> NERVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.NERVES);
    public static final DeferredItem<BlockItem> HEART_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HEART);
    public static final DeferredItem<BlockItem> CAPILLARIES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CAPILLARIES);
    public static final DeferredItem<BlockItem> ARTERIES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ARTERIES);
    public static final DeferredItem<DoubleHighBlockItem> EYEBULB_ITEM = ITEMS.registerItem("eyebulb",
            props -> new DoubleHighBlockItem(ModBlocks.EYEBULB.get(), props));
    public static final DeferredItem<BlockItem> FLESH_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.FLESH_SLAB);
    public static final DeferredItem<BlockItem> ORNATED_CARPET_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ORNATED_CARPET);
    public static final DeferredItem<BlockItem> FLESH_CARPET_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.FLESH_CARPET);
    public static final DeferredItem<BlockItem> RED_CHAIN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RED_CHAIN);
    public static final DeferredItem<BlockItem> BLACK_NYLIUM_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_NYLIUM);
    public static final DeferredItem<BlockItem> BLIGHTSTONE_NYLIUM_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLIGHTSTONE_NYLIUM);
    public static final DeferredItem<BlockItem> LUST_FLOWER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.LUST_FLOWER);
    public static final DeferredItem<DoubleHighBlockItem> TALL_CRIMSON_ROOTS_ITEM = ITEMS.registerItem("tall_crimson_roots",
            props -> new DoubleHighBlockItem(ModBlocks.TALL_CRIMSON_ROOTS.get(), props));
    public static final DeferredItem<DoubleHighBlockItem> TALL_BLACK_GRASS_ITEM = ITEMS.registerItem("tall_black_grass",
            props -> new DoubleHighBlockItem(ModBlocks.TALL_BLACK_GRASS.get(), props));
    public static final DeferredItem<BlockItem> HELL_FORGE_CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HELL_FORGE_CONTROLLER);
    public static final DeferredItem<BlockItem> DEMONIC_CORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DEMONIC_CORE);
    public static final DeferredItem<BlockItem> HELL_FORGE_VAULT_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HELL_FORGE_VAULT);
    /** Right-click a Hell Forge Vault to cycle its mode: input, output, fuel. */
    public static final DeferredItem<dev.zsskayr.merlins_inferno.item.ForgeWrenchItem> FORGE_WRENCH = ITEMS.registerItem("forge_wrench",
            dev.zsskayr.merlins_inferno.item.ForgeWrenchItem::new, new Item.Properties().stacksTo(1));

    // Testing/admin convenience - the Druid otherwise only ever appears via natural spawning in
    // the Hallowed Grove (see ModBiomeProvider). Background/highlight colors: mossy forest green
    // with a warm bark-gold spot pattern, matching the "nature guardian" theme.
    public static final DeferredItem<DeferredSpawnEggItem> IMP_SPAWN_EGG = ITEMS.registerItem("imp_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.IMP, 0xA8231B, 0xF2C230, props));
    public static final DeferredItem<DeferredSpawnEggItem> STARVED_SPAWN_EGG = ITEMS.registerItem("starved_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.STARVED, 0x2A1A16, 0xFF5A1F, props));
    public static final DeferredItem<DeferredSpawnEggItem> GRYMN_SPAWN_EGG = ITEMS.registerItem("grymn_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.GRYMN, 0x1C1626, 0x8FE0D0, props));
    public static final DeferredItem<DeferredSpawnEggItem> DULLAHAN_SPAWN_EGG = ITEMS.registerItem("dullahan_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.DULLAHAN, 0x1B1F2B, 0xD8D2BC, props));
    public static final DeferredItem<DeferredSpawnEggItem> ELIAS_SPAWN_EGG = ITEMS.registerItem("elias_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.ELIAS, 0xD9D2BC, 0x6FE3F5, props));
    public static final DeferredItem<DeferredSpawnEggItem> SACRED_CULTIST_SPAWN_EGG = ITEMS.registerItem("sacred_cultist_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.SACRED_CULTIST, 0x6B5A48, 0xE8D9A8, props));
    public static final DeferredItem<DeferredSpawnEggItem> CINDER_SPAWN_EGG = ITEMS.registerItem("cinder_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.CINDER, 0xA0967B, 0xE85A17, props));
    public static final DeferredItem<DeferredSpawnEggItem> TEMPLAR_SPAWN_EGG = ITEMS.registerItem("templar_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.TEMPLAR, 0xB8BCC4, 0x2E5FA8, props));
    public static final DeferredItem<DeferredSpawnEggItem> SACRED_PRIEST_SPAWN_EGG = ITEMS.registerItem("sacred_priest_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.SACRED_PRIEST, 0xF2EEDC, 0xC9A227, props));
    public static final DeferredItem<DeferredSpawnEggItem> ANDRAS_SPAWN_EGG = ITEMS.registerItem("andras_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.ANDRAS, 0x5A0E0E, 0xC9A227, props));
    public static final DeferredItem<DeferredSpawnEggItem> OSTARA_SPAWN_EGG = ITEMS.registerItem("ostara_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.OSTARA, 0x5FA85F, 0xF2B8D0, props));
    public static final DeferredItem<DeferredSpawnEggItem> DRUID_SPAWN_EGG = ITEMS.registerItem("druid_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.DRUID, 0x4A6B3D, 0xC9A66B, props));

    // --- Circle 1 -> 2: the three boss key items (one per route) and the Pandora Box they are offered to.
    // Andras drops the Book of Contracts, Ostara Eve's Secret, Elias the Flame of God. Placed with the
    // Nether Star in the Pandora Box (see menu.PandoraBoxMenu). ---
    public static final DeferredItem<Item> BOOK_OF_CONTRACTS = ITEMS.registerSimpleItem("book_of_contracts",
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    /** Also edible - Ostara's fruit: an enchanted golden apple's blessings for twenty minutes (but then it is gone as a key). */
    public static final DeferredItem<Item> EVES_SECRET = ITEMS.registerSimpleItem("eves_secret",
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant().food(new FoodProperties.Builder().nutrition(8).saturationModifier(1.2F).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 24000, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 24000, 3), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 24000, 0), 1.0F)
                    .build()));
    /** Drinkable: ten minutes of immunity to Poison. */
    public static final DeferredItem<AntidoteItem> ANTIDOTE = ITEMS.registerItem("antidote",
            props -> new AntidoteItem(props.stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> FLAME_OF_GOD = ITEMS.registerSimpleItem("flame_of_god",
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    // Found in the Sacred Church's chest. Survives death (see event.PandoraHandler).
    public static final DeferredItem<PandoraBoxItem> PANDORA_BOX = ITEMS.registerItem("pandora_box",
            props -> new PandoraBoxItem(ModBlocks.PANDORA_BOX.get(), props.stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    // Forged in the opened Pandora Box (Circle 2 only). Their portals are future content (Circle 2 dimensions).
    public static final DeferredItem<OblivionKeyItem> OBLIVION_KEY = ITEMS.registerItem("oblivion_key",
            props -> new OblivionKeyItem(props.durability(3).rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<PortalKeyItem> FOREST_KEY = ITEMS.registerItem("forest_key",
            props -> new PortalKeyItem(props.stacksTo(1).rarity(Rarity.RARE).fireResistant(), "forest"));

    // --- Marks (item.MarkItem): cursed, soulbound power that binds to its bearer; the Curios slot is "mark". ---
    /** 6% from Cain. What it does lives in the Mark handlers; it is cured only by a Scroll of Salvation. */
    public static final DeferredItem<dev.zsskayr.merlins_inferno.item.MarkItem> MARK_OF_CAIN = ITEMS.registerItem("mark_of_cain",
            props -> new dev.zsskayr.merlins_inferno.item.MarkItem(props.rarity(Rarity.EPIC)));
    /** The only cure for a Mark: a Cleric's expensive trade, or rarely the Sacred Priest's drop. */
    public static final DeferredItem<dev.zsskayr.merlins_inferno.item.ScrollOfSalvationItem> SCROLL_OF_SALVATION = ITEMS.registerItem("scroll_of_salvation",
            props -> new dev.zsskayr.merlins_inferno.item.ScrollOfSalvationItem(props.stacksTo(1).rarity(Rarity.RARE)));

    // --- Circle 1 legendary swords (one per route, equal power): the Hollow Relic (very rare boss/miniboss drop, one use) is the crafting core of each. ---
    public static final DeferredItem<SwordItem> HOLLOW_RELIC = ITEMS.registerItem("hollow_relic",
            props -> new SwordItem(ModTiers.HOLLOW_RELIC, props.rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(ModTiers.HOLLOW_RELIC, 3, -2.4F))));
    /** Dropped by Grymn and Andras, or made from a Book of Contracts (nine per book). */
    public static final DeferredItem<Item> HELL_SEAL = ITEMS.registerSimpleItem("hell_seal", new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant());
    /** An axe, not a sword: heavy and slow, burns what it hits, and bullies the weak - see DebtCollectorHandler. */
    public static final DeferredItem<AxeItem> DEBT_COLLECTOR = ITEMS.registerItem("debt_collector",
            props -> new AxeItem(ModTiers.LEGENDARY, props.rarity(Rarity.EPIC).fireResistant().attributes(AxeItem.createAttributes(ModTiers.LEGENDARY, 7.0F, -3.0F))) {
                @Override
                public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                    target.igniteForSeconds(6);
                    return super.hurtEnemy(stack, target, attacker);
                }

                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.translatable("item.merlins_inferno.debt_collector.tooltip.fire").withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.translatable("item.merlins_inferno.debt_collector.tooltip.evil").withStyle(ChatFormatting.DARK_RED));
                    tooltip.add(Component.translatable("item.merlins_inferno.debt_collector.tooltip.weak").withStyle(ChatFormatting.DARK_RED));
                }
            });
    /** A dagger, not a sword: quick, light hits that poison what they cut. */
    public static final DeferredItem<SwordItem> EDENS_THORN = ITEMS.registerItem("edens_thorn",
            props -> new SwordItem(ModTiers.LEGENDARY, props.rarity(Rarity.EPIC).attributes(SwordItem.createAttributes(ModTiers.LEGENDARY, -2, -1.0F))) {
                @Override
                public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), attacker);
                    return super.hurtEnemy(stack, target, attacker);
                }

                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.translatable("item.merlins_inferno.edens_thorn.tooltip").withStyle(ChatFormatting.DARK_GREEN));
                }
            });
    /** Sanctified II on every hit, Holy I built in, and 1.5x damage to anything stronger than the wielder - see VigilsSorrowHandler. */
    public static final DeferredItem<SwordItem> VIGILS_SORROW = ITEMS.registerItem("vigils_sorrow",
            props -> new SwordItem(ModTiers.LEGENDARY, props.rarity(Rarity.EPIC).attributes(SwordItem.createAttributes(ModTiers.LEGENDARY, 3, -2.4F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    tooltip.add(Component.translatable("item.merlins_inferno.vigils_sorrow.tooltip.sanctified").withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.translatable("item.merlins_inferno.vigils_sorrow.tooltip.holy").withStyle(ChatFormatting.YELLOW));
                    tooltip.add(Component.translatable("item.merlins_inferno.vigils_sorrow.tooltip.strong").withStyle(ChatFormatting.YELLOW));
                }
            });

    // --- Circle 2: Ebony (Cain, the Flesh Golem and the Ebony Debris of Oblivion) and the Dark Coven. The scrap comes from
    // Ebony Debris, rarely from Ancient Debris, and from Oblivion's bosses; the Hell Forge turns it into the bar (one to one).
    // Tools use Withered Bone as the handle, like the other sets; see ModTiers.EBONY / ModArmorMaterials.EBONY. ---
    public static final DeferredItem<Item> EBONY_SCRAP = ITEMS.registerSimpleItem("ebony_scrap", new Item.Properties().fireResistant());
    public static final DeferredItem<Item> EBONY_BAR = ITEMS.registerSimpleItem("ebony_bar", new Item.Properties().fireResistant());
    public static final DeferredItem<BlockItem> BLACK_STEM_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_STEM);
    public static final DeferredItem<BlockItem> BLACK_WART_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_WART_BLOCK);
    public static final DeferredItem<BlockItem> BLACK_FUNGUS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_FUNGUS);
    public static final DeferredItem<BlockItem> BLACK_GRASS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_GRASS);
    public static final DeferredItem<BlockItem> BLACK_VINES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_VINES);
    public static final DeferredItem<BlockItem> OBLIVION_SHROOMLIGHT_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.OBLIVION_SHROOMLIGHT);
    // Dropped by Soul Shroomlight (2-4); four make one back.
    public static final DeferredItem<Item> SOUL_SPORE = ITEMS.registerSimpleItem("soul_spore", new Item.Properties());
    public static final DeferredItem<BlockItem> ASH_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ASH_BLOCK);
    public static final DeferredItem<BlockItem> BLACK_SPROUTS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_SPROUTS);
    public static final DeferredItem<BlockItem> BLACK_ROOTS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BLACK_ROOTS);
    public static final DeferredItem<BlockItem> EBONY_DEBRIS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.EBONY_DEBRIS, new Item.Properties().fireResistant());

    public static final DeferredItem<SwordItem> EBONY_SWORD = ITEMS.registerItem("ebony_sword",
            props -> new SwordItem(ModTiers.EBONY, props.fireResistant().attributes(SwordItem.createAttributes(ModTiers.EBONY, 3, -2.4F))));
    public static final DeferredItem<AxeItem> EBONY_AXE = ITEMS.registerItem("ebony_axe",
            props -> new AxeItem(ModTiers.EBONY, props.fireResistant().attributes(AxeItem.createAttributes(ModTiers.EBONY, 5.0F, -3.0F))));
    public static final DeferredItem<ShovelItem> EBONY_SHOVEL = ITEMS.registerItem("ebony_shovel",
            props -> new ShovelItem(ModTiers.EBONY, props.fireResistant().attributes(ShovelItem.createAttributes(ModTiers.EBONY, 1.5F, -3.0F))));
    public static final DeferredItem<PickaxeItem> EBONY_PICKAXE = ITEMS.registerItem("ebony_pickaxe",
            props -> new PickaxeItem(ModTiers.EBONY, props.fireResistant().attributes(PickaxeItem.createAttributes(ModTiers.EBONY, 1.0F, -2.8F))));
    public static final DeferredItem<HoeItem> EBONY_HOE = ITEMS.registerItem("ebony_hoe",
            props -> new HoeItem(ModTiers.EBONY, props.fireResistant().attributes(HoeItem.createAttributes(ModTiers.EBONY, -4.0F, 0.0F))));

    public static final DeferredItem<ArmorItem> EBONY_HELMET = ITEMS.registerItem("ebony_helmet",
            props -> new ArmorItem(ModArmorMaterials.EBONY, ArmorItem.Type.HELMET, props.fireResistant().durability(ArmorItem.Type.HELMET.getDurability(42))));
    public static final DeferredItem<ArmorItem> EBONY_CHESTPLATE = ITEMS.registerItem("ebony_chestplate",
            props -> new ArmorItem(ModArmorMaterials.EBONY, ArmorItem.Type.CHESTPLATE, props.fireResistant().durability(ArmorItem.Type.CHESTPLATE.getDurability(42))));
    public static final DeferredItem<ArmorItem> EBONY_LEGGINGS = ITEMS.registerItem("ebony_leggings",
            props -> new ArmorItem(ModArmorMaterials.EBONY, ArmorItem.Type.LEGGINGS, props.fireResistant().durability(ArmorItem.Type.LEGGINGS.getDurability(42))));
    public static final DeferredItem<ArmorItem> EBONY_BOOTS = ITEMS.registerItem("ebony_boots",
            props -> new ArmorItem(ModArmorMaterials.EBONY, ArmorItem.Type.BOOTS, props.fireResistant().durability(ArmorItem.Type.BOOTS.getDurability(42))));

    // The Dark Coven, stages I to VII: see item.DarkCovenItem. A MYTHIC sword (not craftable; Cain drops stage I very rarely),
    // separate from the ordinary Ebony Sword; the stages are raised at a smithing table.
    public static final DeferredItem<DarkCovenItem> DARK_COVEN_I = registerDarkCoven("dark_coven", 0);
    public static final DeferredItem<DarkCovenItem> DARK_COVEN_II = registerDarkCoven("dark_coven_2", 1);
    public static final DeferredItem<DarkCovenItem> DARK_COVEN_III = registerDarkCoven("dark_coven_3", 2);
    public static final DeferredItem<DarkCovenItem> DARK_COVEN_IV = registerDarkCoven("dark_coven_4", 3);
    public static final DeferredItem<DarkCovenItem> DARK_COVEN_V = registerDarkCoven("dark_coven_5", 4);
    public static final DeferredItem<DarkCovenItem> DARK_COVEN_VI = registerDarkCoven("dark_coven_6", 5);
    public static final DeferredItem<DarkCovenItem> DARK_COVEN_VII = registerDarkCoven("dark_coven_7", 6);

    private static DeferredItem<DarkCovenItem> registerDarkCoven(String name, int stage) {
        return ITEMS.registerItem(name, props -> new DarkCovenItem(stage, props.rarity(Rarity.EPIC).fireResistant()
                .attributes(SwordItem.createAttributes(ModTiers.MYTHIC, 3 + stage, -2.4F))));
    }

    public static final DeferredItem<DeferredSpawnEggItem> CAIN_SPAWN_EGG = ITEMS.registerItem("cain_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.CAIN, 0x1A1420, 0x7A2BC9, props));
    public static final DeferredItem<DeferredSpawnEggItem> FLESH_GOLEM_SPAWN_EGG = ITEMS.registerItem("flesh_golem_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntityTypes.FLESH_GOLEM, 0x6B2A2A, 0xC9A66B, props));

    // --- Edenweed: Raw edenweed (found on the bush; eating it raw is weak) -> Drying Rack -> Dried Edenweed -> boiled
    // into a Minor Sight Potion, giving the Druidic Trance. ---
    public static final DeferredItem<Item> RAW_EDENWEED = ITEMS.registerSimpleItem("raw_edenweed",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).alwaysEdible()
                    .effect(() -> new MobEffectInstance(ModEffects.DRUIDIC_TRANCE, 400, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 160, 0), 1.0F)
                    .build()));
    public static final DeferredItem<Item> DRIED_EDENWEED = ITEMS.registerSimpleItem("dried_edenweed", new Item.Properties());
    public static final DeferredItem<MinorSightPotionItem> MINOR_SIGHT_POTION = ITEMS.registerItem("minor_sight_potion",
            props -> new MinorSightPotionItem(props.stacksTo(16)));
    public static final DeferredItem<BlockItem> BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.BENCH);

    public static final DeferredItem<BlockItem> DRYING_RACK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DRYING_RACK);

    /** Points at the nearest boss/miniboss instead of world spawn - see ChampionSeekerItem and ModTags.EntityTypes.BOSS_COMPASS_TARGETS. */
    public static final DeferredItem<ChampionSeekerItem> CHAMPION_SEEKER = ITEMS.registerItem("champion_seeker",
            props -> new ChampionSeekerItem(props.stacksTo(1).rarity(Rarity.UNCOMMON)));

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
