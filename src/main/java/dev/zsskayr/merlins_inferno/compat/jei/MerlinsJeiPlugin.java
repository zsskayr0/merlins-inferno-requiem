package dev.zsskayr.merlins_inferno.compat.jei;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.menu.PandoraDisplayRecipes;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;
import mezz.jei.api.constants.RecipeTypes;

/** Shows the Pandora's Box rituals and Hell Forge's exclusive cooking recipes in JEI. */
@JeiPlugin
public class MerlinsJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new PandoraJeiCategory(helper), new HellForgeJeiCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PandoraJeiCategory.TYPE, recipes());
        // Where the Lyrium with no recipe of its own comes from (cluster drops, loot).
        for (String id : new String[] {"lyrium_raw", "lyrium_refined", "red_lyrium_raw", "red_lyrium_refined"}) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, id));
            registration.addIngredientInfo(new ItemStack(item), mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                    net.minecraft.network.chat.Component.translatable("jei.merlins_inferno.info." + id));
        }
        if (Minecraft.getInstance().level != null) {
            registration.addRecipes(HellForgeJeiCategory.TYPE, dev.zsskayr.merlins_inferno.forge.ForgeThermalData.allRecipes(Minecraft.getInstance().level).stream()
                    .map(dev.zsskayr.merlins_inferno.forge.HellForgeDisplayRecipe::new).toList());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.PANDORA_BOX.get()), PandoraJeiCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.HELL_FORGE_CONTROLLER.get()), HellForgeJeiCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.HELL_FORGE_CONTROLLER.get()), RecipeTypes.BLASTING);
    }

    /** Display-only items stay out of JEI's list. */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                List.of(new ItemStack(ModItems.EXCALIBUR_AWAKENING.get()), new ItemStack(ModItems.EXCALIBUR_SLOT.get()),
                        new ItemStack(ModItems.GEHENNA_AWAKENING.get())));
    }

    private static List<PandoraJeiRecipe> recipes() {
        return PandoraDisplayRecipes.ALL.stream()
                .map(entry -> new PandoraJeiRecipe(entry.rule(), entry.catalystStack(), entry.offeringStacks(), entry.result()))
                .toList();
    }
}
