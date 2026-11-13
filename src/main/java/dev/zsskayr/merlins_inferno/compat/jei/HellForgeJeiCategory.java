package dev.zsskayr.merlins_inferno.compat.jei;

import java.util.HashMap;
import java.util.Map;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
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

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.client.ForgePanel;
import dev.zsskayr.merlins_inferno.client.HellForgeRecipeText;
import dev.zsskayr.merlins_inferno.forge.HellForgeDisplayRecipe;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/** A Hellforge smelting page, drawn as a machine panel. */
public final class HellForgeJeiCategory implements IRecipeCategory<HellForgeDisplayRecipe> {
    public static final RecipeType<HellForgeDisplayRecipe> TYPE = RecipeType.create(Merlins_inferno.MODID, "hell_forge_smelting", HellForgeDisplayRecipe.class);
    private final IGuiHelper helper;
    private final IDrawable icon;
    private final IDrawable background;
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    public HellForgeJeiCategory(IGuiHelper helper) {
        this.helper = helper;
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.HELL_FORGE_CONTROLLER.get()));
        background = helper.createBlankDrawable(HellForgeRecipeText.WIDTH, HellForgeRecipeText.HEIGHT);
    }

    @Override public RecipeType<HellForgeDisplayRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.merlins_inferno.hell_forge"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HellForgeDisplayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, HellForgeRecipeText.INPUT_X, HellForgeRecipeText.INPUT_Y)
                .addIngredients(recipe.holder().value().getIngredients().getFirst());
        builder.addSlot(RecipeIngredientRole.OUTPUT, HellForgeRecipeText.OUTPUT_X, HellForgeRecipeText.INPUT_Y)
                .addItemStack(recipe.holder().value().getResultItem(Minecraft.getInstance().level.registryAccess()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, HellForgeRecipeText.DROSS_X, HellForgeRecipeText.INPUT_Y)
                .addItemStack(new ItemStack(ModItems.DUSK_DROSS.get()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(HellForgeRecipeText.drossChance(recipe)));
    }

    @Override
    public void draw(HellForgeDisplayRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        HellForgeRecipeText.draw(graphics, recipe, true);
        arrows.computeIfAbsent(HellForgeRecipeText.ticks(recipe), helper::createAnimatedRecipeArrow)
                .draw(graphics, HellForgeRecipeText.ARROW_X, HellForgeRecipeText.ARROW_Y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, HellForgeDisplayRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (ForgePanel.overGauge(mouseX, mouseY, HellForgeRecipeText.GAUGE_H)) tooltip.addAll(HellForgeRecipeText.gaugeTooltip(recipe));
    }
}
