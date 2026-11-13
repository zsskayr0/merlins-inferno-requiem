package dev.zsskayr.merlins_inferno.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import dev.zsskayr.merlins_inferno.forge.ForgeClientConfig;

/**
 * The Hellforge's recipe pages in JEI and EMI, drawn like machine panels: a raised panel (JEI only - EMI frames every
 * recipe itself), sunken slots and tanks, and a heat gauge on the left whose numbers live in its tooltip.
 */
public final class ForgePanel {
    /** The hottest the Hellforge goes (the Foundry), so every gauge reads on the same scale. */
    public static final double TOP = 5673.15;
    public static final int GAUGE_X = 6, GAUGE_Y = 5, GAUGE_W = 6;

    private ForgePanel() {
    }

    public static void raised(GuiGraphics g, int w, int h) {
        g.fill(0, 0, w, h, 0xFFC6C6C6);
        g.fill(0, 0, w, 1, 0xFFFFFFFF);
        g.fill(0, 0, 1, h, 0xFFFFFFFF);
        g.fill(0, h - 1, w, h, 0xFF555555);
        g.fill(w - 1, 0, w, h, 0xFF555555);
    }

    public static void sunken(GuiGraphics g, int x, int y, int w, int h, int inner) {
        g.fill(x, y, x + w, y + h, inner);
        g.fill(x, y, x + w - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFFFFFFFF);
    }

    /** A slot well around an 18x18 slot whose item sits at (x, y). */
    public static void slot(GuiGraphics g, int x, int y) {
        sunken(g, x - 1, y - 1, 18, 18, 0xFF8B8B8B);
    }

    /**
     * The heat gauge: filled up to {@code min}, with the band from {@code min} to {@code max} brighter on top (the working
     * range); {@code max} equal to {@code min} draws a plain fill.
     */
    public static void gauge(GuiGraphics g, int height, double min, double max, boolean soul) {
        sunken(g, GAUGE_X - 1, GAUGE_Y - 1, GAUGE_W + 2, height + 2, 0xFF2B1A14);
        int low = (int) Math.round(height * Math.min(1, min / TOP)), high = (int) Math.round(height * Math.min(1, max / TOP));
        int bottom = GAUGE_Y + height;
        g.fillGradient(GAUGE_X, bottom - low, GAUGE_X + GAUGE_W, bottom, soul ? 0xFF4FB6D2 : 0xFFE08A3A, soul ? 0xFF1A6A9A : 0xFFB02A10);
        if (high > low) g.fill(GAUGE_X, bottom - high, GAUGE_X + GAUGE_W, bottom - low, soul ? 0xFFB8F4FA : 0xFFFFE08A);
    }

    public static boolean overGauge(double x, double y, int height) {
        return x >= GAUGE_X - 1 && x < GAUGE_X + GAUGE_W + 1 && y >= GAUGE_Y - 1 && y < GAUGE_Y + height + 1;
    }

    /** The gauge's tooltip: the range (or the single point), and a line when only the Blighted Core can reach it. */
    public static List<Component> gaugeTooltip(double min, double max, boolean blighted) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(max > min ? ForgeClientConfig.temperature(min) + " – " + ForgeClientConfig.temperature(max)
                : ForgeClientConfig.temperature(min)));
        if (blighted) lines.add(Component.translatable("block.merlins_inferno.blighted_core"));
        return lines;
    }

    /** Recipe time under the arrow. */
    public static void time(GuiGraphics g, int ticks, int arrowX, int y) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        String text = String.format("%.1f s", ticks / 20.0);
        g.drawString(font, text, arrowX + 12 - font.width(text) / 2, y, 0xFF555555, false);
    }
}
