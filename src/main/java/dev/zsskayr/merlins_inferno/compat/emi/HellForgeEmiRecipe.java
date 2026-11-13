package dev.zsskayr.merlins_inferno.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import dev.zsskayr.merlins_inferno.client.ForgePanel;
import dev.zsskayr.merlins_inferno.client.HellForgeRecipeText;
import dev.zsskayr.merlins_inferno.forge.HellForgeDisplayRecipe;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/** A Hellforge smelting page in EMI, laid out like the JEI panel (EMI draws the frame itself). */
final class HellForgeEmiRecipe extends BasicEmiRecipe {
    private final HellForgeDisplayRecipe recipe;

    HellForgeEmiRecipe(RecipeHolder<AbstractCookingRecipe> holder) {
        super(MerlinsEmiPlugin.HELL_FORGE, MerlinsEmiPlugin.id("/hell_forge/" + holder.id().getNamespace() + "/" + holder.id().getPath()),
                HellForgeRecipeText.WIDTH, HellForgeRecipeText.HEIGHT);
        recipe = new HellForgeDisplayRecipe(holder);
        inputs.add(EmiIngredient.of(holder.value().getIngredients().getFirst()));
        outputs.add(EmiStack.of(holder.value().getResultItem(Minecraft.getInstance().level.registryAccess())));
        outputs.add(EmiStack.of(ModItems.DUSK_DROSS.get()).setChance((float) recipe.material(Minecraft.getInstance().level).drossChance()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, HellForgeRecipeText.WIDTH, HellForgeRecipeText.HEIGHT,
                (graphics, mouseX, mouseY, delta) -> HellForgeRecipeText.draw(graphics, recipe, false));
        widgets.addSlot(inputs.getFirst(), HellForgeRecipeText.INPUT_X - 1, HellForgeRecipeText.INPUT_Y - 1);
        widgets.addFillingArrow(HellForgeRecipeText.ARROW_X, HellForgeRecipeText.ARROW_Y, HellForgeRecipeText.ticks(recipe) * 50);
        widgets.addSlot(outputs.getFirst(), HellForgeRecipeText.OUTPUT_X - 1, HellForgeRecipeText.INPUT_Y - 1).recipeContext(this);
        widgets.addSlot(outputs.get(1), HellForgeRecipeText.DROSS_X - 1, HellForgeRecipeText.INPUT_Y - 1).recipeContext(this);
        widgets.addTooltip((mouseX, mouseY) -> HellForgeRecipeText.gaugeTooltip(recipe).stream()
                        .map(c -> ClientTooltipComponent.create(c.getVisualOrderText())).toList(),
                ForgePanel.GAUGE_X - 1, ForgePanel.GAUGE_Y - 1, ForgePanel.GAUGE_W + 2, HellForgeRecipeText.GAUGE_H + 2);
    }
}
