package dev.zsskayr.merlins_inferno.client;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import dev.zsskayr.merlins_inferno.menu.HellForgeMenu;

/**
 * The Hell Forge's own custom panel art ({@code hell_forge.png}), replacing vanilla's furnace
 * texture entirely. Every progress indicator is custom-drawn on top of it:
 * <ul>
 *     <li>the fuel tank ({@link #renderFuelBar}) - a horizontal gauge that grows outward from the
 *     center orb toward both edges as the tank fills, using the user-supplied
 *     {@code hell_forge_bar_fill.png} (its own transparent gap/margins already match the panel's
 *     baked-in track, so an empty tank just shows that dark track underneath, unfilled). That
 *     center orb isn't decoration - it's the fuel slot's own socket, baked right into the gauge
 *     it feeds (see {@link HellForgeMenu#FUEL_SLOT}'s coordinates), not off to the side;</li>
 *     <li>the cook-progress indicator ({@link #renderBurnIcon}) - an 11-frame gauge
 *     ({@code hell_forge_burn_p0.png}..{@code p10.png}) sitting between the input/output sockets,
 *     starting gray (p10, idle) and turning a stronger red as a frame approaches p0 (done).</li>
 * </ul>
 * The panel already matches vanilla's own standard 176x166 layout for the player-inventory grid
 * (same {@code 8 + col*18}/{@code 84 + row*18}/{@code 142} coordinates), so unlike the previous
 * vanilla-furnace-texture version, no extra offset or title strip is needed - the whole GUI is
 * exactly as tall as the art. The "Hell Forge" title is intentionally not drawn at all (see
 * {@link #renderLabels}); the panel is meant to be read without one.
 */
public class HellForgeScreen extends AbstractContainerScreen<HellForgeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("merlins_inferno", "textures/gui/hellforge/hell_forge.png");
    private static final ResourceLocation BAR_FILL = ResourceLocation.fromNamespaceAndPath("merlins_inferno", "textures/gui/hellforge/hell_forge_bar_fill.png");
    private static final ResourceLocation[] BURN_FRAMES = new ResourceLocation[11];
    static {
        for (int i = 0; i <= 10; i++) {
            BURN_FRAMES[i] = ResourceLocation.fromNamespaceAndPath("merlins_inferno", "textures/gui/hellforge/hell_forge_burn_p" + i + ".png");
        }
    }

    // hell_forge.png is a 256x256 sheet with the actual 176x166 panel baked into its top-left
    // corner - same convention as vanilla's own furnace.png.
    private static final int TEXTURE_SHEET_SIZE = 256;

    // Fuel tank bar - hell_forge_bar_fill.png at its native 172x20 size, positioned so its two
    // colored segments land exactly on the dark track baked into the panel art (sampled by hand
    // against hell_forge.png: track interior runs from panel x=11-73 and x=100-163, y=24-31).
    private static final int BAR_X = 2;
    private static final int BAR_Y = 18;
    private static final int BAR_TEX_WIDTH = 172;
    private static final int BAR_TEX_HEIGHT = 20;
    // Each segment's extent within the bar_fill texture itself (0-indexed, inclusive).
    private static final int BAR_LEFT_SEG_START = 9;
    private static final int BAR_LEFT_SEG_END = 70;
    private static final int BAR_RIGHT_SEG_START = 99;
    private static final int BAR_RIGHT_SEG_END = 161;

    // Cook-progress gauge - sits centered between the input/output sockets, same 16x16 size as
    // every hell_forge_burn_p*.png frame, so it's blit 1:1 with no scaling.
    private static final int BURN_X = 79;
    private static final int BURN_Y = 50;
    private static final int BURN_SIZE = 16;

    public HellForgeScreen(HellForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_SHEET_SIZE, TEXTURE_SHEET_SIZE);

        renderFuelBar(guiGraphics, x + BAR_X, y + BAR_Y);
        renderBurnIcon(guiGraphics, x + BURN_X, y + BURN_Y);
    }

    /** Grows outward from the center orb toward both edges as {@link HellForgeMenu#getFuelLevel} rises. */
    private void renderFuelBar(GuiGraphics guiGraphics, int barLeft, int barTop) {
        float level = this.menu.getFuelLevel();
        if (level <= 0.0F) {
            return;
        }

        int leftVisible = Math.round(level * (BAR_LEFT_SEG_END - BAR_LEFT_SEG_START + 1));
        if (leftVisible > 0) {
            int clipLeft = barLeft + BAR_LEFT_SEG_END + 1 - leftVisible;
            int clipRight = barLeft + BAR_LEFT_SEG_END + 1;
            guiGraphics.enableScissor(clipLeft, barTop, clipRight, barTop + BAR_TEX_HEIGHT);
            guiGraphics.blit(BAR_FILL, barLeft, barTop, 0, 0, BAR_TEX_WIDTH, BAR_TEX_HEIGHT, BAR_TEX_WIDTH, BAR_TEX_HEIGHT);
            guiGraphics.disableScissor();
        }

        int rightVisible = Math.round(level * (BAR_RIGHT_SEG_END - BAR_RIGHT_SEG_START + 1));
        if (rightVisible > 0) {
            int clipLeft = barLeft + BAR_RIGHT_SEG_START;
            int clipRight = clipLeft + rightVisible;
            guiGraphics.enableScissor(clipLeft, barTop, clipRight, barTop + BAR_TEX_HEIGHT);
            guiGraphics.blit(BAR_FILL, barLeft, barTop, 0, 0, BAR_TEX_WIDTH, BAR_TEX_HEIGHT, BAR_TEX_WIDTH, BAR_TEX_HEIGHT);
            guiGraphics.disableScissor();
        }
    }

    /** Idle/no-progress shows p10 (gray); the frame reddens as it counts down to p0 (done). */
    private void renderBurnIcon(GuiGraphics guiGraphics, int left, int top) {
        int frame = 10 - Math.round(this.menu.getCookProgress() * 10);
        frame = Math.max(0, Math.min(10, frame));
        guiGraphics.blit(BURN_FRAMES[frame], left, top, 0, 0, BURN_SIZE, BURN_SIZE, BURN_SIZE, BURN_SIZE);
    }

    /** Skips the "Hell Forge" title entirely - the panel's own art carries the GUI, not a label. */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // No extra this.renderTooltip(...) call here - AbstractContainerScreen.render already ends
        // with one (that's what draws hovered-slot tooltips); adding another just draws the same
        // thing twice on top of itself every frame.
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int barLeft = this.leftPos + BAR_X;
        int barTop = this.topPos + BAR_Y;
        if (mouseX >= barLeft && mouseX < barLeft + BAR_TEX_WIDTH && mouseY >= barTop && mouseY < barTop + BAR_TEX_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    List.of(Component.translatable("gui.merlins_inferno.hell_forge.fuel",
                            this.menu.getStoredFuel(), this.menu.getFuelCapacity())),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
