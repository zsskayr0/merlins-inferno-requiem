package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
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

    // --- Ironwood material set: see ModTiers.IRONWOOD / ModArmorMaterials.IRONWOOD for the
    // balance rationale (mines like Diamond, hits/blocks like Iron, outlasts both). ---

    // Plain crafting material - no in-world use yet beyond crafting the tools/armor below.
    public static final DeferredItem<Item> IRONWOOD_BAR = ITEMS.registerSimpleItem("ironwood_bar", new Item.Properties());

    public static final DeferredItem<SwordItem> IRONWOOD_SWORD = ITEMS.registerItem("ironwood_sword",
            props -> new SwordItem(ModTiers.IRONWOOD, props.attributes(SwordItem.createAttributes(ModTiers.IRONWOOD, 3, -2.4F))));
    public static final DeferredItem<AxeItem> IRONWOOD_AXE = ITEMS.registerItem("ironwood_axe",
            props -> new AxeItem(ModTiers.IRONWOOD, props.attributes(AxeItem.createAttributes(ModTiers.IRONWOOD, 6.0F, -3.1F))));
    public static final DeferredItem<ShovelItem> IRONWOOD_SHOVEL = ITEMS.registerItem("ironwood_shovel",
            props -> new ShovelItem(ModTiers.IRONWOOD, props.attributes(ShovelItem.createAttributes(ModTiers.IRONWOOD, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> IRONWOOD_HOE = ITEMS.registerItem("ironwood_hoe",
            props -> new HoeItem(ModTiers.IRONWOOD, props.attributes(HoeItem.createAttributes(ModTiers.IRONWOOD, -2.0F, -1.0F))));

    public static final DeferredItem<ArmorItem> IRONWOOD_HELMET = ITEMS.registerItem("ironwood_helmet",
            props -> new ArmorItem(ModArmorMaterials.IRONWOOD, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(40))));
    public static final DeferredItem<ArmorItem> IRONWOOD_CHESTPLATE = ITEMS.registerItem("ironwood_chestplate",
            props -> new ArmorItem(ModArmorMaterials.IRONWOOD, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(40))));
    public static final DeferredItem<ArmorItem> IRONWOOD_LEGGINGS = ITEMS.registerItem("ironwood_leggings",
            props -> new ArmorItem(ModArmorMaterials.IRONWOOD, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(40))));
    public static final DeferredItem<ArmorItem> IRONWOOD_BOOTS = ITEMS.registerItem("ironwood_boots",
            props -> new ArmorItem(ModArmorMaterials.IRONWOOD, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(40))));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
