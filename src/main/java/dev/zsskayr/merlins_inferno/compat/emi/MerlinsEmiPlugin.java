package dev.zsskayr.merlins_inferno.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.menu.PandoraDisplayRecipes;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

@EmiEntrypoint
public final class MerlinsEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory PANDORA = new EmiRecipeCategory(id("pandora_box"), EmiStack.of(ModItems.PANDORA_BOX.get()));
    public static final EmiRecipeCategory HELL_FORGE = new EmiRecipeCategory(id("hell_forge_smelting"), EmiStack.of(ModBlocks.HELL_FORGE_CONTROLLER.get()));

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, path); }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(PANDORA);
        registry.addWorkstation(PANDORA, EmiStack.of(ModItems.PANDORA_BOX.get()));
        for (PandoraDisplayRecipes.Entry entry : PandoraDisplayRecipes.ALL) {
            registry.addRecipe(new PandoraEmiRecipe(entry));
        }

        registry.addCategory(HELL_FORGE);
        registry.addWorkstation(HELL_FORGE, EmiStack.of(ModBlocks.HELL_FORGE_CONTROLLER.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(ModBlocks.HELL_FORGE_CONTROLLER.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.BLASTING, EmiStack.of(ModBlocks.HELL_FORGE_CONTROLLER.get()));
        for (RecipeHolder<dev.zsskayr.merlins_inferno.recipe.HellForgeRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(ModRecipeTypes.HELL_FORGE_SMELTING.get())) {
            registry.addRecipe(new HellForgeEmiRecipe(holder));
        }
        registry.removeEmiStacks(EmiStack.of(ModItems.EXCALIBUR_AWAKENING.get()));
        registry.removeEmiStacks(EmiStack.of(ModItems.EXCALIBUR_SLOT.get()));
    }
}
