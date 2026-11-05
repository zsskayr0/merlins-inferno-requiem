package dev.zsskayr.merlins_inferno.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.zsskayr.merlins_inferno.recipe.HellForgeRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;

final class HellForgeEmiRecipe extends BasicEmiRecipe {
    HellForgeEmiRecipe(RecipeHolder<HellForgeRecipe> holder) {
        super(MerlinsEmiPlugin.HELL_FORGE, holder.id(), 92, 46);
        inputs.add(EmiIngredient.of(holder.value().getIngredients().getFirst()));
        outputs.add(EmiStack.of(holder.value().getResultItem(Minecraft.getInstance().level.registryAccess())));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.getFirst(), 10, 13);
        widgets.addFillingArrow(36, 13, 200);
        widgets.addSlot(outputs.getFirst(), 66, 13).recipeContext(this);
    }
}
