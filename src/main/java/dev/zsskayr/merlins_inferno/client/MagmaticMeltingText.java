package dev.zsskayr.merlins_inferno.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;

/** A Magmatic melting page: heat gauge, the stone, an animated arrow with the time, a tall tank of the fluid. */
public final class MagmaticMeltingText {
    public static final int WIDTH = 150, HEIGHT = 60, GAUGE_H = 50;
    public static final int TANK_X = 128, TANK_Y = 5, TANK_W = 14, TANK_H = 50;
    public static final int INPUT_X = 30, INPUT_Y = 21, ARROW_X = 60, ARROW_Y = 21;

    private MagmaticMeltingText() {
    }

    public static List<ItemStack> inputs(ForgeThermalData.Melting melting) {
        List<ItemStack> stacks = new ArrayList<>();
        for (String id : melting.items()) BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).ifPresent(item -> stacks.add(new ItemStack(item)));
        if (!melting.tag().isEmpty()) {
            BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, ResourceLocation.parse(melting.tag())))
                    .forEach(holder -> stacks.add(new ItemStack(holder.value())));
        }
        return stacks;
    }

    public static Fluid fluid(ForgeThermalData.Melting melting) {
        return BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(melting.fluid())).orElse(null);
    }

    /** Everything but the arrow (each viewer animates its own). {@code panel} draws the raised panel and the slot well. */
    public static void draw(GuiGraphics g, ForgeThermalData.Melting melting, boolean panel) {
        if (panel) {
            ForgePanel.raised(g, WIDTH, HEIGHT);
            ForgePanel.slot(g, INPUT_X, INPUT_Y);
        }
        ForgePanel.gauge(g, GAUGE_H, melting.min(), melting.min(), melting.minCoreTier() >= 2);
        ForgePanel.sunken(g, TANK_X - 1, TANK_Y - 1, TANK_W + 2, TANK_H + 2, 0xFF2B2B2B);
        ForgePanel.time(g, melting.time(), ARROW_X, ARROW_Y + 19);
    }

    public static List<Component> gaugeTooltip(ForgeThermalData.Melting melting) {
        return ForgePanel.gaugeTooltip(melting.min(), melting.min(), melting.minCoreTier() >= 2);
    }

    /** How long the arrow takes to fill: the melting's own time. */
    public static int arrowMillis(ForgeThermalData.Melting melting) {
        return Math.max(1000, melting.time() * 50);
    }
}
