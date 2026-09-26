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
 * Design (per project decision): {@code ROWANWOOD}'s defense and toughness match Iron exactly;
 * the higher-than-Diamond durability lives on the per-piece multiplier passed in
 * {@link ModItems} (see {@code ArmorItem.Type#getDurability(int)}), not here.
 * <p>
 * Worn-armor textures live at {@code textures/models/armor/rowanwood_layer_1.png} (and
 * {@code _layer_2.png} for leggings).
 */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Merlins_inferno.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROWANWOOD = ARMOR_MATERIALS.register("rowanwood", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 2);
                map.put(ArmorItem.Type.LEGGINGS, 5);
                map.put(ArmorItem.Type.CHESTPLATE, 6);
                map.put(ArmorItem.Type.HELMET, 2);
                map.put(ArmorItem.Type.BODY, 5);
            }),
            25, // enchantment value, matches Gold's armor enchantability (design doc: Gold-level enchantability)
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.ROWANWOOD_BAR.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "rowanwood"))),
            0.0F, // toughness, matches Iron
            0.0F  // knockback resistance, matches Iron
    ));

    /**
     * Design (per project decision): Demonblood armor is slightly superior to Netherite in defense
     * (chestplate 9, leggings 7, total 22 vs 20) with Netherite-equivalent toughness and knockback resistance,
     * higher enchantability (22 vs 15), and equal durability (multiplier 37).
     * Worn-armor textures live at {@code textures/models/armor/demonblood_layer_1.png} (and
     * {@code _layer_2.png} for leggings).
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DEMONBLOOD = ARMOR_MATERIALS.register("demonblood", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 7);
                map.put(ArmorItem.Type.CHESTPLATE, 9);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 7);
            }),
            22, // enchantment value, matches ModTiers.DEMONBLOOD (Netherite is 15)
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(ModItems.DEMONBLOOD_BAR.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "demonblood"))),
            3.0F, // toughness, matches Netherite
            0.1F  // knockback resistance, matches Netherite
    ));

    /**
     * Design (per project decision): Seraphium is meant to be strictly the best of the 3 armor
     * sets - 24 total defense (4/9/7/4) vs Demonblood's 22, toughness 4 vs 3, same knockback
     * resistance. (It used to be Diamond's exact 20 with toughness 2, i.e. weaker than Demonblood.) Durability multiplier lives in {@link ModItems} like Rowanwood's does.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SERAPHIUM = ARMOR_MATERIALS.register("seraphium", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 4);
                map.put(ArmorItem.Type.LEGGINGS, 7);
                map.put(ArmorItem.Type.CHESTPLATE, 9);
                map.put(ArmorItem.Type.HELMET, 4);
                map.put(ArmorItem.Type.BODY, 7);
            }),
            25, // enchantment value, above Iron/Rowanwood's 9
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            () -> Ingredient.of(ModItems.SERAPHIUM_BAR.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "seraphium"))),
            4.0F, // toughness, above Demonblood's/Netherite's 3
            0.1F  // knockback resistance, matches Netherite/Demonblood
    ));

    private ModArmorMaterials() {
    }

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }
}
