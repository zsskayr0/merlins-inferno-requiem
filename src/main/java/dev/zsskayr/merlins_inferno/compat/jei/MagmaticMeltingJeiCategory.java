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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.client.ForgePanel;
import dev.zsskayr.merlins_inferno.client.MagmaticMeltingText;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** The Magmatic Core's melting, drawn as a machine panel: heat gauge, stone, arrow, a tall tank of lava or Soul Lava. */
public final class MagmaticMeltingJeiCategory implements IRecipeCategory<ForgeThermalData.Melting> {
    public static final RecipeType<ForgeThermalData.Melting> TYPE = RecipeType.create(Merlins_inferno.MODID, "magmatic_melting", ForgeThermalData.Melting.class);
    private final IGuiHelper helper;
    private final IDrawable icon;
    private final IDrawable background;
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    public MagmaticMeltingJeiCategory(IGuiHelper helper) {
        this.helper = helper;
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.MAGMATIC_CORE.get()));
        background = helper.createBlankDrawable(MagmaticMeltingText.WIDTH, MagmaticMeltingText.HEIGHT);
    }

    @Override public RecipeType<ForgeThermalData.Melting> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.merlins_inferno.magmatic_melting"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ForgeThermalData.Melting melting, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, MagmaticMeltingText.INPUT_X, MagmaticMeltingText.INPUT_Y)
                .addItemStacks(MagmaticMeltingText.inputs(melting));
        var fluid = MagmaticMeltingText.fluid(melting);
        if (fluid != null) builder.addSlot(RecipeIngredientRole.OUTPUT, MagmaticMeltingText.TANK_X, MagmaticMeltingText.TANK_Y)
                .setFluidRenderer(melting.amount(), false, MagmaticMeltingText.TANK_W, MagmaticMeltingText.TANK_H)
                .addFluidStack(fluid, melting.amount());
    }

    @Override
    public void draw(ForgeThermalData.Melting melting, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        MagmaticMeltingText.draw(graphics, melting, true);
        arrows.computeIfAbsent(melting.time(), helper::createAnimatedRecipeArrow)
                .draw(graphics, MagmaticMeltingText.ARROW_X, MagmaticMeltingText.ARROW_Y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ForgeThermalData.Melting melting, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (ForgePanel.overGauge(mouseX, mouseY, MagmaticMeltingText.GAUGE_H)) tooltip.addAll(MagmaticMeltingText.gaugeTooltip(melting));
    }
}
