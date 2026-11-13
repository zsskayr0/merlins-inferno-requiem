package dev.zsskayr.merlins_inferno.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import dev.zsskayr.merlins_inferno.client.ForgePanel;
import dev.zsskayr.merlins_inferno.client.MaterialsTableText;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;

/** One row of the Hellforge's materials table in EMI, laid out like the JEI panel. */
final class HellForgeMaterialEmiRecipe extends BasicEmiRecipe {
    private final ForgeThermalData.MaterialEntry entry;

    HellForgeMaterialEmiRecipe(ForgeThermalData.MaterialEntry entry, int index) {
        super(MerlinsEmiPlugin.HELL_FORGE_MATERIALS, MerlinsEmiPlugin.id("/hell_forge_materials/" + index), MaterialsTableText.WIDTH, MaterialsTableText.HEIGHT);
        this.entry = entry;
        inputs.add(EmiIngredient.of(entry.items().stream().map(EmiStack::of).toList()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, MaterialsTableText.WIDTH, MaterialsTableText.HEIGHT,
                (graphics, mouseX, mouseY, delta) -> MaterialsTableText.draw(graphics, entry, false));
        widgets.addSlot(inputs.getFirst(), MaterialsTableText.ITEM_X - 1, MaterialsTableText.ITEM_Y - 1);
        widgets.addTooltip((mouseX, mouseY) -> MaterialsTableText.gaugeTooltip(entry).stream()
                        .map(c -> ClientTooltipComponent.create(c.getVisualOrderText())).toList(),
                ForgePanel.GAUGE_X - 1, ForgePanel.GAUGE_Y - 1, ForgePanel.GAUGE_W + 2, MaterialsTableText.GAUGE_H + 2);
    }
}
