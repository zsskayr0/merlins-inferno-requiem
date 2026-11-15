package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModPotions;

/**
 * Elixir of Warding is brewed in two steps: Water Bottle + Refined Lyrium gives a Lyrium-Infused
 * Potion, then Angelic Essence turns that into the Elixir.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class WardingBrewing {
    private WardingBrewing() {
    }

    @SubscribeEvent
    public static void onRegisterBrewing(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        builder.addMix(Potions.WATER, ModItems.LYRIUM_REFINED.get(), ModPotions.LYRIUM_INFUSED);
        Ingredient infused = DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS,
                new PotionContents(ModPotions.LYRIUM_INFUSED), Items.POTION);
        builder.addRecipe(new BrewingRecipe(infused, Ingredient.of(ModItems.ANGELIC_ESSENCE.get()),
                new ItemStack(ModItems.WARDING_ELIXIR.get())));
    }
}
