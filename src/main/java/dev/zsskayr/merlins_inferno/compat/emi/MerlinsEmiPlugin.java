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
    public static final EmiRecipeCategory MAGMATIC_MELTING = new EmiRecipeCategory(id("magmatic_melting"), EmiStack.of(ModBlocks.MAGMATIC_CORE.get()));
    public static final EmiRecipeCategory HELL_FORGE_MATERIALS = new EmiRecipeCategory(id("hell_forge_materials"), EmiStack.of(ModBlocks.DEMONIC_CORE.get()));

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
        registry.addWorkstation(HELL_FORGE, EmiStack.of(ModBlocks.BLIGHTED_HELLFORGE_CONTROLLER.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.BLASTING, EmiStack.of(ModBlocks.HELL_FORGE_CONTROLLER.get()));
        for (var holder : dev.zsskayr.merlins_inferno.forge.ForgeThermalData.allRecipes(net.minecraft.client.Minecraft.getInstance().level)) {
            registry.addRecipe(new HellForgeEmiRecipe(holder));
        }
        registry.addCategory(MAGMATIC_MELTING);
        registry.addWorkstation(MAGMATIC_MELTING, EmiStack.of(ModBlocks.MAGMATIC_CORE.get()));
        var meltings = dev.zsskayr.merlins_inferno.forge.ForgeThermalData.meltings(net.minecraft.client.Minecraft.getInstance().level);
        for (int i = 0; i < meltings.size(); i++) {
            registry.addRecipe(new MagmaticMeltingEmiRecipe(meltings.get(i), i));
        }
        registry.addCategory(HELL_FORGE_MATERIALS);
        registry.addWorkstation(HELL_FORGE_MATERIALS, EmiStack.of(ModBlocks.HELL_FORGE_CONTROLLER.get()));
        registry.addWorkstation(HELL_FORGE_MATERIALS, EmiStack.of(ModBlocks.BLIGHTED_HELLFORGE_CONTROLLER.get()));
        var table = dev.zsskayr.merlins_inferno.forge.ForgeThermalData.materialTable(net.minecraft.client.Minecraft.getInstance().level);
        for (int i = 0; i < table.size(); i++) {
            registry.addRecipe(new HellForgeMaterialEmiRecipe(table.get(i), i));
        }
        // Where the Lyrium with no recipe of its own comes from (cluster drops, loot).
        for (String name : new String[] {"lyrium_raw", "lyrium_refined", "red_lyrium_raw", "red_lyrium_refined"}) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id(name));
            registry.addRecipe(new dev.emi.emi.api.recipe.EmiInfoRecipe(
                    java.util.List.of(EmiStack.of(item)),
                    java.util.List.of(net.minecraft.network.chat.Component.translatable("jei.merlins_inferno.info." + name)),
                    id("/info/" + name)));
        }
        registry.removeEmiStacks(EmiStack.of(ModItems.EXCALIBUR_AWAKENING.get()));
        registry.removeEmiStacks(EmiStack.of(ModItems.EXCALIBUR_SLOT.get()));
        registry.removeEmiStacks(EmiStack.of(ModItems.GEHENNA_AWAKENING.get()));
    }
}
