package dev.zsskayr.merlins_inferno.forge;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

public record HellForgeDisplayRecipe(RecipeHolder<AbstractCookingRecipe> holder) {
    public ItemStack input() {
        ItemStack[] items = holder.value().getIngredients().getFirst().getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }
    public ForgeThermalData.Material material(Level level) {
        return ForgeThermalData.material(level, input(), holder.id());
    }
}
