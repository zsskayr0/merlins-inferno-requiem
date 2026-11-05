package dev.zsskayr.merlins_inferno.menu;

import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Ingredient;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Recipe;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Display data for both recipe viewers; the quantities and results come from the real ritual rules. */
public final class PandoraDisplayRecipes {
    public record Entry(Recipe rule, Ingredient catalyst, List<Ingredient> offerings) {
        public ItemStack catalystStack() { return stack(catalyst); }
        public List<ItemStack> offeringStacks() {
            return offerings.stream().map(offering -> {
                ItemStack stack = stack(offering);
                stack.setCount(PandoraRecipeRules.required(rule, offering));
                return stack;
            }).toList();
        }
        public ItemStack result() { return PandoraBoxMenu.result(rule); }
    }

    public static final List<Entry> ALL = List.of(
            new Entry(Recipe.AWAKENING, Ingredient.STAR, List.of(Ingredient.BOOK, Ingredient.EVE, Ingredient.FLAME)),
            new Entry(Recipe.FOREST, Ingredient.RUNE, List.of(Ingredient.FAE, Ingredient.INFERNAL, Ingredient.ANGELIC)),
            new Entry(Recipe.OBLIVION, Ingredient.RUNE, List.of(Ingredient.NETHERITE, Ingredient.INFERNAL, Ingredient.FAE)),
            new Entry(Recipe.ELYSIUM, Ingredient.BOOK, List.of(Ingredient.NETHERITE, Ingredient.ANGELIC, Ingredient.FAE)),
            new Entry(Recipe.SERAPH_WINGS, Ingredient.TATTERED_WINGS, List.of(Ingredient.NETHERITE, Ingredient.ANGELIC, Ingredient.FAE)),
            new Entry(Recipe.FALLEN_WINGS, Ingredient.TATTERED_WINGS, List.of(Ingredient.NETHERITE, Ingredient.INFERNAL, Ingredient.FAE)));

    private PandoraDisplayRecipes() {}

    private static ItemStack stack(Ingredient ingredient) {
        return switch (ingredient) {
            case BOOK -> new ItemStack(ModItems.BOOK_OF_CONTRACTS.get());
            case EVE -> new ItemStack(ModItems.EVES_SECRET.get());
            case FLAME -> new ItemStack(ModItems.FLAME_OF_GOD.get());
            case STAR -> new ItemStack(Items.NETHER_STAR);
            case FAE -> new ItemStack(ModItems.FAE_ESSENCE.get());
            case INFERNAL -> new ItemStack(ModItems.INFERNAL_ESSENCE.get());
            case ANGELIC -> new ItemStack(ModItems.ANGELIC_ESSENCE.get());
            case RUNE -> new ItemStack(ModItems.PRIMARY_CIRCLE_RUNE.get());
            case NETHERITE -> new ItemStack(Items.NETHERITE_INGOT);
            case TATTERED_WINGS -> new ItemStack(ModItems.TATTERED_WINGS.get());
            default -> throw new IllegalArgumentException(ingredient.name());
        };
    }
}
