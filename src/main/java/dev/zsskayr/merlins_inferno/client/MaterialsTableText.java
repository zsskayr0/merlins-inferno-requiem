package dev.zsskayr.merlins_inferno.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;

/** A materials-table row: heat gauge (the working range), the item, its name. */
public final class MaterialsTableText {
    public static final int WIDTH = 150, HEIGHT = 30, GAUGE_H = 20;
    public static final int ITEM_X = 22, ITEM_Y = 7;

    private MaterialsTableText() {
    }

    public static void draw(GuiGraphics g, ForgeThermalData.MaterialEntry entry, boolean panel) {
        var material = entry.material();
        if (panel) {
            ForgePanel.raised(g, WIDTH, HEIGHT);
            ForgePanel.slot(g, ITEM_X, ITEM_Y);
        }
        ForgePanel.gauge(g, GAUGE_H, material.min(), material.max(), false);
        g.drawString(Minecraft.getInstance().font, entry.items().getFirst().getHoverName(), ITEM_X + 22, ITEM_Y + 4, 0xFF3A2A20, false);
    }

    public static List<Component> gaugeTooltip(ForgeThermalData.MaterialEntry entry) {
        var material = entry.material();
        return ForgePanel.gaugeTooltip(material.min(), material.max(), material.minCoreTier() >= 2);
    }
}
