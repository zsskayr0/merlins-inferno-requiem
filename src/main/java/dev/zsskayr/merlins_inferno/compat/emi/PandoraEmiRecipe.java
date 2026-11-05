package dev.zsskayr.merlins_inferno.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.zsskayr.merlins_inferno.menu.PandoraDisplayRecipes;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Recipe;
import net.minecraft.network.chat.Component;

final class PandoraEmiRecipe extends BasicEmiRecipe {
    private final Recipe rule;

    PandoraEmiRecipe(PandoraDisplayRecipes.Entry entry) {
        super(MerlinsEmiPlugin.PANDORA, MerlinsEmiPlugin.id("pandora_box/" + entry.rule().name().toLowerCase(java.util.Locale.ROOT)), 126, 56);
        rule = entry.rule();
        inputs.add(EmiStack.of(entry.catalystStack()));
        entry.offeringStacks().forEach(stack -> inputs.add(EmiStack.of(stack)));
        if (!entry.result().isEmpty()) outputs.add(EmiStack.of(entry.result()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addText(Component.translatable("jei.merlins_inferno.pandora_box.catalyst"), 0, 4, 0xFF808080, false);
        widgets.addText(Component.translatable("jei.merlins_inferno.pandora_box.offerings"), 28, 4, 0xFF808080, false);
        widgets.addSlot(inputs.get(0), 2, 20);
        for (int i = 1; i < inputs.size(); i++) widgets.addSlot(inputs.get(i), 28 + (i - 1) * 20, 20);
        widgets.addText(Component.literal("->"), 88, 24, 0xFF808080, false);
        if (!outputs.isEmpty()) widgets.addSlot(outputs.getFirst(), 106, 20).recipeContext(this);
        widgets.addText(Component.translatable(rule == Recipe.AWAKENING
                ? "jei.merlins_inferno.pandora_box.awakening" : "jei.merlins_inferno.pandora_box.requires_opened"),
                0, 44, 0xFF8A2B2B, false);
    }
}
