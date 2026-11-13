package dev.zsskayr.merlins_inferno.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;

import dev.zsskayr.merlins_inferno.forge.HellForgeDisplayRecipe;

/** A Hellforge smelting page: heat gauge (the working range), input, animated arrow with the time, result and dross. */
public final class HellForgeRecipeText {
    public static final int WIDTH = 150, HEIGHT = 46, GAUGE_H = 36;
    public static final int INPUT_X = 28, INPUT_Y = 14, ARROW_X = 52, ARROW_Y = 14, OUTPUT_X = 86, DROSS_X = 120;

    private HellForgeRecipeText() {
    }

    public static int ticks(HellForgeDisplayRecipe recipe) {
        var value = recipe.holder().value();
        return value.getType() == RecipeType.BLASTING ? value.getCookingTime() : Math.max(1, value.getCookingTime() / 2);
    }

    public static void draw(GuiGraphics g, HellForgeDisplayRecipe recipe, boolean panel) {
        var material = recipe.material(Minecraft.getInstance().level);
        if (panel) {
            ForgePanel.raised(g, WIDTH, HEIGHT);
            ForgePanel.slot(g, INPUT_X, INPUT_Y);
            ForgePanel.slot(g, OUTPUT_X, INPUT_Y);
            ForgePanel.slot(g, DROSS_X, INPUT_Y);
        }
        ForgePanel.gauge(g, GAUGE_H, material.min(), material.max(), false);
        ForgePanel.time(g, ticks(recipe), ARROW_X, ARROW_Y + 19);
    }

    public static List<Component> gaugeTooltip(HellForgeDisplayRecipe recipe) {
        var material = recipe.material(Minecraft.getInstance().level);
        return ForgePanel.gaugeTooltip(material.min(), material.max(), material.minCoreTier() >= 2);
    }

    public static Component drossChance(HellForgeDisplayRecipe recipe) {
        return Component.translatable("gui.merlins_inferno.forge.dross_chance",
                Math.round(recipe.material(Minecraft.getInstance().level).drossChance() * 100));
    }
}
