package dev.zsskayr.merlins_inferno.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import dev.zsskayr.merlins_inferno.forge.HellForgeDisplayRecipe;
import dev.zsskayr.merlins_inferno.forge.ForgeClientConfig;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;

final class HellForgeEmiRecipe extends BasicEmiRecipe {
    private final HellForgeDisplayRecipe recipe;
    HellForgeEmiRecipe(RecipeHolder<AbstractCookingRecipe> holder) {
        super(MerlinsEmiPlugin.HELL_FORGE, MerlinsEmiPlugin.id("/hell_forge/" + holder.id().getNamespace() + "/" + holder.id().getPath()), 164, 66);
        recipe = new HellForgeDisplayRecipe(holder);
        inputs.add(EmiIngredient.of(holder.value().getIngredients().getFirst()));
        outputs.add(EmiStack.of(holder.value().getResultItem(Minecraft.getInstance().level.registryAccess())));
        outputs.add(EmiStack.of(ModItems.DUSK_DROSS.get()).setChance((float) recipe.material(Minecraft.getInstance().level).drossChance()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.getFirst(), 10, 13);
        widgets.addFillingArrow(36, 13, 200);
        widgets.addSlot(outputs.getFirst(), 94, 13).recipeContext(this);
        widgets.addSlot(outputs.get(1), 136, 13).recipeContext(this);
        widgets.addDrawable(0, 36, 164, 30, (graphics, mouseX, mouseY, delta) -> {
            var client = Minecraft.getInstance();
            var material = recipe.material(client.level);
            graphics.drawString(client.font, ForgeClientConfig.cyclingRange(material, System.currentTimeMillis()), 3, 2, 0xFF995522, false);
            graphics.drawString(client.font, net.minecraft.network.chat.Component.translatable("gui.merlins_inferno.forge.dross_chance",
                    Math.round(material.drossChance() * 100)), 3, 16, 0xFF775588, false);
        });
    }
}
