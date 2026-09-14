package dev.zsskayr.merlins_inferno.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModRecipeSerializers;
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;

/**
 * A cooking recipe only the Hell Forge will ever look up - vanilla furnaces/blast furnaces/smokers
 * only ever query {@code RecipeType.SMELTING}/{@code BLASTING}/{@code SMOKING}, and this recipe
 * lives under its own {@link ModRecipeTypes#HELL_FORGE_SMELTING} instead, so they never see it.
 * Otherwise identical to vanilla's own {@code SmeltingRecipe} - same fields, same
 * {@link AbstractCookingRecipe} plumbing (matching/assembling/experience/cooking time) - only the
 * type/serializer differ. See {@code HellForgeBlockEntity#getRecipe} for the lookup itself.
 */
public class HellForgeRecipe extends AbstractCookingRecipe {
    public HellForgeRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(ModRecipeTypes.HELL_FORGE_SMELTING.get(), group, category, ingredient, result, experience, cookingTime);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.HELL_FORGE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.HELL_FORGE_SMELTING.get();
    }
}
