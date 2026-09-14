package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.recipe.HellForgeRecipe;

/**
 * {@link RecipeType}s this mod adds. Just {@link #HELL_FORGE_SMELTING} for now - see
 * {@link HellForgeRecipe}'s javadoc for why it needs its own type instead of reusing vanilla's
 * smelting/blasting.
 */
public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Merlins_inferno.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<HellForgeRecipe>> HELL_FORGE_SMELTING = RECIPE_TYPES.register("hell_forge_smelting",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "hell_forge_smelting")));

    private ModRecipeTypes() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
    }
}
