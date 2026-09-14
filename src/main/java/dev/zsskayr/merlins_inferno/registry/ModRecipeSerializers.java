package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.recipe.HellForgeRecipe;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Merlins_inferno.MODID);

    // 200 = the default cooking time if a recipe JSON omits "cookingtime", matching vanilla's own
    // smelting serializer default.
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCookingSerializer<HellForgeRecipe>> HELL_FORGE_SMELTING = RECIPE_SERIALIZERS.register(
            "hell_forge_smelting", () -> new SimpleCookingSerializer<>(HellForgeRecipe::new, 200));

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
