package dev.zsskayr.merlins_inferno.compat.jei;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.recipe.HellForgeRecipe;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class HellForgeJeiCategory implements IRecipeCategory<HellForgeRecipe> {
    public static final RecipeType<HellForgeRecipe> TYPE = RecipeType.create(Merlins_inferno.MODID, "hell_forge_smelting", HellForgeRecipe.class);
    private final IDrawable icon;
    private final IDrawable background;

    public HellForgeJeiCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.HELL_FORGE_CONTROLLER.get()));
        background = helper.createBlankDrawable(92, 46);
    }

    @Override public RecipeType<HellForgeRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.merlins_inferno.hell_forge"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HellForgeRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(10, 13).addIngredients(recipe.getIngredients().getFirst());
        builder.addOutputSlot(66, 13).addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }
}
