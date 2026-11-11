package dev.zsskayr.merlins_inferno.compat.jei;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.forge.HellForgeDisplayRecipe;
import dev.zsskayr.merlins_inferno.forge.ForgeClientConfig;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
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

public final class HellForgeJeiCategory implements IRecipeCategory<HellForgeDisplayRecipe> {
    public static final RecipeType<HellForgeDisplayRecipe> TYPE = RecipeType.create(Merlins_inferno.MODID, "hell_forge_smelting", HellForgeDisplayRecipe.class);
    private final IDrawable icon;
    private final IDrawable background;

    public HellForgeJeiCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.HELL_FORGE_CONTROLLER.get()));
        background = helper.createBlankDrawable(164, 66);
    }

    @Override public RecipeType<HellForgeDisplayRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.merlins_inferno.hell_forge"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HellForgeDisplayRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(10, 13).addIngredients(recipe.holder().value().getIngredients().getFirst());
        builder.addOutputSlot(94, 13).addItemStack(recipe.holder().value().getResultItem(Minecraft.getInstance().level.registryAccess()));
        builder.addOutputSlot(136, 13).addItemStack(new ItemStack(ModItems.DUSK_DROSS.get()));
    }

    @Override public void draw(HellForgeDisplayRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var client = Minecraft.getInstance();
        var material = recipe.material(client.level);
        graphics.drawString(client.font, ForgeClientConfig.cyclingRange(material, System.currentTimeMillis()), 3, 38, 0xFF995522, false);
        graphics.drawString(client.font, Component.translatable("gui.merlins_inferno.forge.dross_chance",
                Math.round(material.drossChance() * 100)), 3, 52, 0xFF775588, false);
    }
}
