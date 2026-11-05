package dev.zsskayr.merlins_inferno.client;

import dev.zsskayr.merlins_inferno.menu.PandoraLayout;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Recipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Static crop of the real Pandora menu, shared by JEI and EMI recipe pages. */
public final class PandoraRecipePreview {
    public static final int WIDTH = PandoraLayout.WIDTH;
    public static final int HEIGHT = 166;
    public static final int RESULT_X = 191;
    public static final int RESULT_Y = 70;
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "merlins_inferno", "textures/gui/pandora/pandora_background.png");
    private static final ResourceLocation SLOTS = ResourceLocation.fromNamespaceAndPath(
            "merlins_inferno", "textures/gui/pandora/hell_forge_old.png");

    private PandoraRecipePreview() {}

    public static void render(GuiGraphics graphics, int x, int y, Recipe rule) {
        graphics.blit(BACKGROUND, x, y, WIDTH, HEIGHT, 0, 0, 1536, 664, 1536, 1024);
        graphics.blit(BACKGROUND, x + 4, y + 22, WIDTH - 8, 137, 24, 96, 940, 532, 1536, 1024);

        for (int step = 0; step < 160; step++) {
            double angle = step * Math.PI / 80;
            int px = x + PandoraLayout.CENTER_X + (int) Math.round(40 * Math.cos(angle));
            int py = y + PandoraLayout.CENTER_Y + (int) Math.round(40 * Math.sin(angle));
            graphics.fill(px, py, px + 1, py + 1, step % 10 < 2 ? 0xFF735145 : 0xFF49464D);
        }

        for (int i = 0; i < 3; i++) {
            graphics.blit(SLOTS, x + PandoraLayout.slotX(i, PandoraLayout.INITIAL_ANGLE) - 1,
                    y + PandoraLayout.slotY(i, PandoraLayout.INITIAL_ANGLE) - 1,
                    7, 83, 18, 18, 256, 256);
        }
        frame(graphics, x + PandoraLayout.CENTER_X - 15, y + PandoraLayout.CENTER_Y - 15, 30, 30);
        graphics.drawString(Minecraft.getInstance().font, Component.literal("→"), x + 165, y + 73, 0xFFBBA992, false);
        frame(graphics, x + RESULT_X - 6, y + RESULT_Y - 6, 30, 30);
        frame(graphics, x + PandoraLayout.BUTTON_X, y + PandoraLayout.BUTTON_Y,
                PandoraLayout.BUTTON_WIDTH, PandoraLayout.BUTTON_HEIGHT);

        var font = Minecraft.getInstance().font;
        Component title = Component.translatable("jei.merlins_inferno.pandora_box");
        graphics.drawString(font, title, x + (WIDTH - font.width(title)) / 2, y + 7, 0xFFDFDDE0, false);
        Component caption = Component.translatable(rule == Recipe.AWAKENING
                ? "jei.merlins_inferno.pandora_box.awakening_short"
                : "jei.merlins_inferno.pandora_box.requires_opened_short");
        graphics.drawString(font, caption, x + (WIDTH - font.width(caption)) / 2, y + 140, 0xFFDFDDE0, false);
    }

    private static void frame(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF17171B);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF87868C);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF33343B);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFF9A5143);
        graphics.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF321D21);
    }
}
