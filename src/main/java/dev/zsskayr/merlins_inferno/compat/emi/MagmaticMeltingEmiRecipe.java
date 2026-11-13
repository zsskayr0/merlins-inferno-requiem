package dev.zsskayr.merlins_inferno.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import dev.zsskayr.merlins_inferno.client.ForgePanel;
import dev.zsskayr.merlins_inferno.client.MagmaticMeltingText;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;

/** One Magmatic melting page in EMI, laid out like the JEI panel (EMI draws the frame itself). */
final class MagmaticMeltingEmiRecipe extends BasicEmiRecipe {
    private final ForgeThermalData.Melting melting;

    MagmaticMeltingEmiRecipe(ForgeThermalData.Melting melting, int index) {
        super(MerlinsEmiPlugin.MAGMATIC_MELTING, MerlinsEmiPlugin.id("/magmatic_melting/" + index), MagmaticMeltingText.WIDTH, MagmaticMeltingText.HEIGHT);
        this.melting = melting;
        inputs.add(EmiIngredient.of(MagmaticMeltingText.inputs(melting).stream().map(EmiStack::of).toList()));
        var fluid = MagmaticMeltingText.fluid(melting);
        if (fluid != null) outputs.add(EmiStack.of(fluid, melting.amount()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, MagmaticMeltingText.WIDTH, MagmaticMeltingText.HEIGHT,
                (graphics, mouseX, mouseY, delta) -> MagmaticMeltingText.draw(graphics, melting, false));
        widgets.addSlot(inputs.getFirst(), MagmaticMeltingText.INPUT_X - 1, MagmaticMeltingText.INPUT_Y - 1);
        widgets.addFillingArrow(MagmaticMeltingText.ARROW_X, MagmaticMeltingText.ARROW_Y, MagmaticMeltingText.arrowMillis(melting));
        if (!outputs.isEmpty()) widgets.addTank(outputs.getFirst(), MagmaticMeltingText.TANK_X - 1, MagmaticMeltingText.TANK_Y - 1,
                MagmaticMeltingText.TANK_W + 2, MagmaticMeltingText.TANK_H + 2, melting.amount()).drawBack(false).recipeContext(this);
        widgets.addTooltip((mouseX, mouseY) -> MagmaticMeltingText.gaugeTooltip(melting).stream()
                        .map(c -> ClientTooltipComponent.create(c.getVisualOrderText())).toList(),
                ForgePanel.GAUGE_X - 1, ForgePanel.GAUGE_Y - 1, ForgePanel.GAUGE_W + 2, MagmaticMeltingText.GAUGE_H + 2);
    }
}
