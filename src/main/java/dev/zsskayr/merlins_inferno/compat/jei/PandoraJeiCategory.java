package dev.zsskayr.merlins_inferno.compat.jei;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PandoraJeiCategory implements IRecipeCategory<PandoraJeiRecipe> {
    public static final RecipeType<PandoraJeiRecipe> TYPE = RecipeType.create(Merlins_inferno.MODID, "pandora_box", PandoraJeiRecipe.class);
    private static final int WIDTH = 126;
    private static final int HEIGHT = 56;

    private final IDrawable icon;
    private final IDrawable background;

    public PandoraJeiCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.PANDORA_BOX.get()));
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<PandoraJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.merlins_inferno.pandora_box");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PandoraJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 2, 20).addItemStack(recipe.catalyst());
        for (int i = 0; i < recipe.offerings().size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 28 + i * 20, 20).addItemStack(recipe.offerings().get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 20).addItemStack(recipe.result());
    }

    @Override
    public void draw(PandoraJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.merlins_inferno.pandora_box.catalyst"), 0, 4, 0xFF808080, false);
        graphics.drawString(font, Component.translatable("jei.merlins_inferno.pandora_box.offerings"), 28, 4, 0xFF808080, false);
        graphics.drawString(font, Component.literal("->"), 88, 24, 0xFF808080, false);
        graphics.drawString(font, Component.translatable("jei.merlins_inferno.pandora_box.requires_opened"), 0, 44, 0xFF8A2B2B, false);
    }
}
