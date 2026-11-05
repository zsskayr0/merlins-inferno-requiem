package dev.zsskayr.merlins_inferno.compat.jei;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.client.PandoraRecipePreview;
import dev.zsskayr.merlins_inferno.menu.PandoraLayout;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PandoraJeiCategory implements IRecipeCategory<PandoraJeiRecipe> {
    public static final RecipeType<PandoraJeiRecipe> TYPE = RecipeType.create(Merlins_inferno.MODID, "pandora_box", PandoraJeiRecipe.class);
    private final IDrawable icon;
    private final IDrawable background;

    public PandoraJeiCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.PANDORA_BOX.get()));
        this.background = helper.createBlankDrawable(PandoraRecipePreview.WIDTH, PandoraRecipePreview.HEIGHT);
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
        builder.addSlot(recipe.reusableCatalyst() ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT,
                PandoraLayout.CENTER_X - 9, PandoraLayout.CENTER_Y - 9)
                .addItemStack(recipe.catalyst());
        for (int i = 0; i < recipe.offerings().size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT,
                    PandoraLayout.slotX(i, PandoraLayout.INITIAL_ANGLE) - 1,
                    PandoraLayout.slotY(i, PandoraLayout.INITIAL_ANGLE) - 1)
                    .addItemStack(recipe.offerings().get(i));
        }
        if (!recipe.result().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, PandoraRecipePreview.RESULT_X - 1,
                    PandoraRecipePreview.RESULT_Y - 1).addItemStack(recipe.result());
        }
    }

    @Override
    public void draw(PandoraJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        PandoraRecipePreview.render(graphics, 0, 0, recipe.rule());
    }
}
