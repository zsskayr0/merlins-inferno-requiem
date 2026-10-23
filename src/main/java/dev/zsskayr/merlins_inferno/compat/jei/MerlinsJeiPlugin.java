package dev.zsskayr.merlins_inferno.compat.jei;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Ingredient;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Recipe;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shows the Pandora's Box rituals (rules in code, not recipe JSON) in JEI. */
@JeiPlugin
public class MerlinsJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new PandoraJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PandoraJeiCategory.TYPE, recipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.PANDORA_BOX.get()), PandoraJeiCategory.TYPE);
    }

    private static List<PandoraJeiRecipe> recipes() {
        List<PandoraJeiRecipe> list = new ArrayList<>();
        list.add(recipe(Recipe.FOREST, new ItemStack(ModItems.FOREST_KEY.get()), Ingredient.VOID,
                Ingredient.OTHERWORLD, Ingredient.INFERNAL, Ingredient.CELESTIAL));
        list.add(recipe(Recipe.OBLIVION, new ItemStack(ModItems.OBLIVION_KEY.get()), Ingredient.BOOK,
                Ingredient.NETHERITE, Ingredient.INFERNAL, Ingredient.OTHERWORLD));
        list.add(recipe(Recipe.SERAPH_WINGS, new ItemStack(ModItems.SERAPH_WINGS.get()), Ingredient.TATTERED_WINGS,
                Ingredient.NETHERITE, Ingredient.CELESTIAL, Ingredient.OTHERWORLD));
        list.add(recipe(Recipe.FALLEN_WINGS, new ItemStack(ModItems.FALLEN_WINGS.get()), Ingredient.TATTERED_WINGS,
                Ingredient.NETHERITE, Ingredient.INFERNAL, Ingredient.OTHERWORLD));
        return list;
    }

    private static PandoraJeiRecipe recipe(Recipe rule, ItemStack result, Ingredient catalyst, Ingredient... offerings) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Ingredient offering : offerings) {
            ItemStack stack = stack(offering);
            stack.setCount(PandoraRecipeRules.required(rule, offering));
            stacks.add(stack);
        }
        return new PandoraJeiRecipe(rule, stack(catalyst), stacks, result);
    }

    private static ItemStack stack(Ingredient ingredient) {
        return switch (ingredient) {
            case BOOK -> new ItemStack(ModItems.BOOK_OF_CONTRACTS.get());
            case OTHERWORLD -> new ItemStack(ModItems.OTHERWORLD_ESSENCE.get());
            case INFERNAL -> new ItemStack(ModItems.INFERNAL_ESSENCE.get());
            case CELESTIAL -> new ItemStack(ModItems.CELESTIAL_ESSENCE.get());
            case VOID -> new ItemStack(ModBlocks.WITHERED_OBSIDIAN.get());
            case NETHERITE -> new ItemStack(Items.NETHERITE_INGOT);
            case TATTERED_WINGS -> new ItemStack(ModItems.TATTERED_WINGS.get());
            default -> throw new IllegalArgumentException(ingredient.name());
        };
    }
}
