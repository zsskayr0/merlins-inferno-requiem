package dev.zsskayr.merlins_inferno.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.client.ForgePanel;
import dev.zsskayr.merlins_inferno.client.MaterialsTableText;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** The Hellforge's materials table: each material with its heat gauge, drawn as a machine panel. */
public final class HellForgeMaterialsJeiCategory implements IRecipeCategory<ForgeThermalData.MaterialEntry> {
    public static final RecipeType<ForgeThermalData.MaterialEntry> TYPE = RecipeType.create(Merlins_inferno.MODID, "hell_forge_materials", ForgeThermalData.MaterialEntry.class);
    private final IDrawable icon;
    private final IDrawable background;

    public HellForgeMaterialsJeiCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.DEMONIC_CORE.get()));
        background = helper.createBlankDrawable(MaterialsTableText.WIDTH, MaterialsTableText.HEIGHT);
    }

    @Override public RecipeType<ForgeThermalData.MaterialEntry> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.merlins_inferno.hell_forge_materials"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ForgeThermalData.MaterialEntry entry, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, MaterialsTableText.ITEM_X, MaterialsTableText.ITEM_Y).addItemStacks(entry.items());
    }

    @Override
    public void draw(ForgeThermalData.MaterialEntry entry, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        MaterialsTableText.draw(graphics, entry, true);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ForgeThermalData.MaterialEntry entry, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (ForgePanel.overGauge(mouseX, mouseY, MaterialsTableText.GAUGE_H)) tooltip.addAll(MaterialsTableText.gaugeTooltip(entry));
    }
}
