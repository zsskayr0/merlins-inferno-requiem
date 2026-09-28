package dev.zsskayr.merlins_inferno.menu;

/** Pure recipe rules shared by preview and server execution; offering order never matters. */
public final class PandoraRecipeRules {
    public enum Ingredient { EMPTY, OTHER, BOOK, EVE, FLAME, STAR, OTHERWORLD, INFERNAL, CELESTIAL, VOID, NETHERITE }
    public enum Recipe { NONE, AWAKENING, OBLIVION, PURGATORY }
    public static final int ESSENCE_COST = 16;
    private PandoraRecipeRules() {}

    /** How many of an offering the recipe consumes (and needs) from its slot; surplus stays in the slot. */
    public static int required(Recipe recipe, Ingredient offering) {
        return switch (recipe) {
            case OBLIVION -> ESSENCE_COST;
            case PURGATORY -> offering == Ingredient.NETHERITE ? 3 : ESSENCE_COST;
            default -> 1;
        };
    }

    public static Recipe match(boolean opened, Ingredient catalyst, Ingredient[] offerings, int[] counts) {
        if (offerings.length != 3 || counts.length != 3) return Recipe.NONE;
        if (!opened && catalyst == Ingredient.STAR && distinct(Recipe.AWAKENING, offerings, counts, Ingredient.BOOK, Ingredient.EVE, Ingredient.FLAME)) return Recipe.AWAKENING;
        if (opened && catalyst == Ingredient.VOID && distinct(Recipe.OBLIVION, offerings, counts, Ingredient.OTHERWORLD, Ingredient.INFERNAL, Ingredient.CELESTIAL)) return Recipe.OBLIVION;
        if (opened && catalyst == Ingredient.BOOK && distinct(Recipe.PURGATORY, offerings, counts, Ingredient.NETHERITE, Ingredient.INFERNAL, Ingredient.OTHERWORLD)) return Recipe.PURGATORY;
        return Recipe.NONE;
    }

    private static boolean distinct(Recipe recipe, Ingredient[] items, int[] counts, Ingredient a, Ingredient b, Ingredient c) {
        int mask = 0;
        for (int i = 0; i < 3; i++) {
            int bit = items[i] == a ? 1 : items[i] == b ? 2 : items[i] == c ? 4 : 0;
            if (bit == 0 || (mask & bit) != 0 || counts[i] < required(recipe, items[i])) return false;
            mask |= bit;
        }
        return mask == 7;
    }
}
