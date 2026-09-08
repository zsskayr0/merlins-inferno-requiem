package dev.zsskayr.merlins_inferno.registry;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
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

    // Starter tome handed out for the future onboarding/tutorial flow. Plain item for now -
    // no reading/GUI behavior yet, just something to register end-to-end and show in-world.
    public static final DeferredItem<Item> GRIMMORIUM = ITEMS.registerSimpleItem("grimmorium", new Item.Properties());

    // --- Rowanwood material set: see ModTiers.ROWANWOOD / ModArmorMaterials.ROWANWOOD for the
    // balance rationale (mines like Diamond, hits/blocks like Iron, outlasts both). ---

    // Plain crafting material - no in-world use yet beyond crafting the tools/armor below.
    public static final DeferredItem<Item> ROWANWOOD_BAR = ITEMS.registerSimpleItem("rowanwood_bar", new Item.Properties());

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

    // --- Demonite material set: see ModTiers.DEMONITE for the balance rationale (Netherite's
    // stats, higher enchantability, +10% damage vs "humans" via DemoniteCombatHandler - called
    // out below in each tool's tooltip). Tool attack attributes mirror vanilla's actual Netherite
    // tool values 1:1. Every demonite_*.json recipe also crafts its result pre-enchanted with
    // Bane of Humanity I (see data/merlins_inferno/recipe/demonite_*.json's result.components). ---

    // Plain crafting material - no in-world use yet beyond crafting the tools below.
    public static final DeferredItem<Item> DEMONITE_BAR = ITEMS.registerSimpleItem("demonite_bar", new Item.Properties());

    public static final DeferredItem<SwordItem> DEMONITE_SWORD = ITEMS.registerItem("demonite_sword",
            props -> new SwordItem(ModTiers.DEMONITE, props.attributes(SwordItem.createAttributes(ModTiers.DEMONITE, 3, -2.4F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });
    public static final DeferredItem<AxeItem> DEMONITE_AXE = ITEMS.registerItem("demonite_axe",
            props -> new AxeItem(ModTiers.DEMONITE, props.attributes(AxeItem.createAttributes(ModTiers.DEMONITE, 5.0F, -3.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });
    public static final DeferredItem<ShovelItem> DEMONITE_SHOVEL = ITEMS.registerItem("demonite_shovel",
            props -> new ShovelItem(ModTiers.DEMONITE, props.attributes(ShovelItem.createAttributes(ModTiers.DEMONITE, 1.5F, -3.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });
    public static final DeferredItem<PickaxeItem> DEMONITE_PICKAXE = ITEMS.registerItem("demonite_pickaxe",
            props -> new PickaxeItem(ModTiers.DEMONITE, props.attributes(PickaxeItem.createAttributes(ModTiers.DEMONITE, 1.0F, -2.8F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });
    public static final DeferredItem<HoeItem> DEMONITE_HOE = ITEMS.registerItem("demonite_hoe",
            props -> new HoeItem(ModTiers.DEMONITE, props.attributes(HoeItem.createAttributes(ModTiers.DEMONITE, -4.0F, 0.0F))) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, tooltip, flag);
                    appendHumanDamageTooltip(tooltip);
                }
            });

    /** Shared tooltip line for every Demonite tool - see {@code DemoniteCombatHandler}. */
    private static void appendHumanDamageTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("item.merlins_inferno.demonite_tooltip.humans_bonus").withStyle(ChatFormatting.DARK_RED));
    }

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
