package dev.zsskayr.merlins_inferno.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.zsskayr.merlins_inferno.client.PandoraRecipePreview;
import dev.zsskayr.merlins_inferno.menu.PandoraDisplayRecipes;
import dev.zsskayr.merlins_inferno.menu.PandoraLayout;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Recipe;

final class PandoraEmiRecipe extends BasicEmiRecipe {
    private final Recipe rule;

    PandoraEmiRecipe(PandoraDisplayRecipes.Entry entry) {
        super(MerlinsEmiPlugin.PANDORA, MerlinsEmiPlugin.id("pandora_box/" + entry.rule().name().toLowerCase(java.util.Locale.ROOT)),
                PandoraRecipePreview.WIDTH, PandoraRecipePreview.HEIGHT);
        rule = entry.rule();
        inputs.add(EmiStack.of(entry.catalystStack()));
        entry.offeringStacks().forEach(stack -> inputs.add(EmiStack.of(stack)));
        if (!entry.result().isEmpty()) outputs.add(EmiStack.of(entry.result()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, PandoraRecipePreview.WIDTH, PandoraRecipePreview.HEIGHT,
                (graphics, x, y, delta) -> PandoraRecipePreview.render(graphics, x, y, rule));
        widgets.addSlot(inputs.getFirst(), PandoraLayout.CENTER_X - 9, PandoraLayout.CENTER_Y - 9).drawBack(false);
        for (int i = 0; i < 3; i++) {
            widgets.addSlot(inputs.get(i + 1), PandoraLayout.slotX(i, PandoraLayout.INITIAL_ANGLE) - 1,
                    PandoraLayout.slotY(i, PandoraLayout.INITIAL_ANGLE) - 1).drawBack(false);
        }
        if (!outputs.isEmpty()) widgets.addSlot(outputs.getFirst(), PandoraRecipePreview.RESULT_X - 1,
                PandoraRecipePreview.RESULT_Y - 1).drawBack(false).recipeContext(this);
    }
}
