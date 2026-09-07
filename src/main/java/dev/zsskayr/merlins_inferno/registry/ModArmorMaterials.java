package dev.zsskayr.merlins_inferno.registry;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Custom armor materials. {@code ArmorMaterial} lives in its own built-in registry
 * ({@code Registries.ARMOR_MATERIAL}), so it gets a {@link DeferredRegister} of its own just like
 * items/blocks/entity types do, rather than being a plain constant.
 * <p>
 * Design (per project decision): {@code IRONWOOD}'s defense and toughness match Iron exactly;
 * the higher-than-Diamond durability lives on the per-piece multiplier passed in
 * {@link ModItems} (see {@code ArmorItem.Type#getDurability(int)}), not here.
 * <p>
 * <b>Known gap:</b> {@code layers} below points at
 * {@code textures/models/armor/ironwood_layer_1.png} (and {@code _layer_2.png} for leggings),
 * which don't exist yet. The inventory icon (from {@code textures/item/}) works fine already;
 * only the worn/equipped look is a placeholder until those two textures are painted.
 */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Merlins_inferno.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> IRONWOOD = ARMOR_MATERIALS.register("ironwood", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 2);
                map.put(ArmorItem.Type.LEGGINGS, 5);
                map.put(ArmorItem.Type.CHESTPLATE, 6);
                map.put(ArmorItem.Type.HELMET, 2);
                map.put(ArmorItem.Type.BODY, 5);
            }),
            9, // enchantment value, matches Iron
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.IRONWOOD_BAR.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "ironwood"))),
            0.0F, // toughness, matches Iron
            0.0F  // knockback resistance, matches Iron
    ));

    private ModArmorMaterials() {
    }

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }
}
