package dev.zsskayr.merlins_inferno.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.menu.PandoraBoxMenu;

/**
 * The Pandora Box's own screen: three circles around a central one, drawn procedurally (no panel art yet - see
 * the design doc's "animations and starry things" note, left for a future art pass). The centre swallows the
 * other three when the ritual is performed.
 * <p>
 * The ritual button arms on the first click and only fires on a second, explicit confirmation - the mandatory
 * warning the design calls for, so the kill is never a surprise.
 */
public class PandoraBoxScreen extends AbstractContainerScreen<PandoraBoxMenu> {
    private static final int PANEL_W = 176, PANEL_H = 200;
    private static final int RADIUS_KEY = 15, RADIUS_STAR = 20;
    private static final int[] KEY_COLORS = {0x8B1A1A, 0x3E8B3E, 0xC9A227};

    private Button ritualButton;
    private boolean armed;

    public PandoraBoxScreen(PandoraBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = PANEL_H - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos, y = this.topPos;
        this.ritualButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.merlins_inferno.pandora_box.ritual"),
                btn -> this.onRitualClick())
                .bounds(x + 28, y + 96, 120, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.merlins_inferno.pandora_box.forge_oblivion"),
                btn -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, PandoraBoxMenu.BUTTON_OBLIVION_KEY))
                .bounds(x + 8, y + 118, 78, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.merlins_inferno.pandora_box.forge_purgatory"),
                btn -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, PandoraBoxMenu.BUTTON_PURGATORY_KEY))
                .bounds(x + 90, y + 118, 78, 20).build());
    }

    private void onRitualClick() {
        if (!this.armed) {
            this.armed = true;
            this.ritualButton.setMessage(Component.translatable("gui.merlins_inferno.pandora_box.ritual_confirm").withStyle(ChatFormatting.RED));
            return;
        }
        this.armed = false;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, PandoraBoxMenu.BUTTON_RITUAL);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean ready = this.menu.isRitualReady() && this.menu.getCircle() < ProgressionHelper.SECOND_CIRCLE;
        this.ritualButton.active = ready;
        if (!ready && this.armed) {
            this.armed = false;
        }
        if (!this.armed) {
            this.ritualButton.setMessage(Component.translatable("gui.merlins_inferno.pandora_box.ritual"));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        guiGraphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xE0100418);
        guiGraphics.fill(x + 2, y + 2, x + PANEL_W - 2, y + PANEL_H - 2, 0xE01A0C28);

        long time = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            int[] pos = PandoraBoxMenu.SLOT_POS[i];
            drawRing(guiGraphics, x + pos[0] + 8, y + pos[1] + 8, RADIUS_KEY, KEY_COLORS[i], this.menu.getSlot(i).hasItem());
        }
        int[] star = PandoraBoxMenu.SLOT_POS[3];
        boolean ready = this.menu.isRitualReady();
        int pulse = ready ? 0x30 + (int) (0x60 * (0.5 + 0.5 * Math.sin(time / 200.0))) : 0x20;
        drawRing(guiGraphics, x + star[0] + 8, y + star[1] + 8, RADIUS_STAR, (pulse << 24) | 0xB090FF, ready);

        Component warning = Component.translatable("gui.merlins_inferno.pandora_box.warning").withStyle(ChatFormatting.DARK_GRAY);
        guiGraphics.drawCenteredString(this.font, warning, x + PANEL_W / 2, y + 6, 0xFFFFFF);
    }

    /** A thin outline (a filled disc when {@code lit}) - not real "stellar" art, just enough to read the layout. */
    private static void drawRing(GuiGraphics guiGraphics, int cx, int cy, int radius, int argb, boolean lit) {
        int inner = lit ? 0 : radius - 3;
        for (int dy = -radius; dy <= radius; dy++) {
            int span = (int) Math.sqrt((double) radius * radius - (double) dy * dy);
            int innerSpan = dy >= -inner && dy <= inner ? (int) Math.sqrt(Math.max(0, inner * inner - dy * dy)) : span + 1;
            if (span <= 0) {
                continue;
            }
            if (innerSpan > 0) {
                guiGraphics.fill(cx - span, cy + dy, cx - Math.min(innerSpan, span), cy + dy + 1, argb);
                guiGraphics.fill(cx + Math.min(innerSpan, span), cy + dy, cx + span, cy + dy + 1, argb);
            } else {
                guiGraphics.fill(cx - span, cy + dy, cx + span, cy + dy + 1, argb);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xE0E0E0, false);
    }
}
