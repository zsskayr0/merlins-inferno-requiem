package dev.zsskayr.merlins_inferno.compat.jei;

import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** One Pandora's Box ritual as JEI shows it: the catalyst, the three offerings (with the counts the rules demand) and the result. */
public record PandoraJeiRecipe(PandoraRecipeRules.Recipe rule, ItemStack catalyst, List<ItemStack> offerings, ItemStack result) {
}
