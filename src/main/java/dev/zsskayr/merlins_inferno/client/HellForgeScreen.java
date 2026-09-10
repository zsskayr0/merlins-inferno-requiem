package dev.zsskayr.merlins_inferno.client;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

import dev.zsskayr.merlins_inferno.menu.HellForgeMenu;

/**
 * Reuses vanilla's furnace GUI for the background/slots (no custom panel art yet), but every
 * progress indicator is custom:
 * <ul>
 *     <li>the fuel tank ({@link #renderFuelBar}) - animated lava, clipped to the fuel level, framed
 *     with the user-supplied tick overlay and a bevel border;</li>
 *     <li>the cook-progress indicator ({@link #renderSmile}) - a hand-drawn demonic grin
 *     ({@link #SMILE}) revealed left-to-right instead of vanilla's flame/arrow, styled after the
 *     DragonForge mod's furnace GUI (dragon head icon) but with a grin instead of a dragon.</li>
 * </ul>
 * The panel is {@link HellForgeMenu#Y_OFFSET} taller than vanilla's furnace GUI so there's room for
 * a title bar above the slots - that extra strip is filled/bordered by hand in {@link #renderBg}
 * (rather than left blank) so the title reads as part of the panel instead of floating above it,
 * and centered horizontally per the title's actual width.
 * <p>
 * The fuel slot no longer lines up with any pre-drawn slot art in the furnace texture (it moved
 * next to the tank bar - see {@link HellForgeMenu}), so {@link #renderFuelSlotFrame} draws a small
 * stylized socket behind it instead of relying on the background image for that one slot.
 */
public class HellForgeScreen extends AbstractContainerScreen<HellForgeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation BAR_OVERLAY = ResourceLocation.fromNamespaceAndPath("merlins_inferno", "textures/gui/hell_forge_fuel_bar.png");
    private static final ResourceLocation SMILE = ResourceLocation.fromNamespaceAndPath("merlins_inferno", "textures/gui/hell_forge_smile.png");
    private static final ResourceLocation LAVA_STILL = ResourceLocation.withDefaultNamespace("block/lava_still");

    private static final int Y_OFFSET = HellForgeMenu.Y_OFFSET;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 16;
    private static final int BAR_WIDTH = 16;
    private static final int BAR_HEIGHT = 58;

    private static final int SMILE_WIDTH = 32;
    private static final int SMILE_HEIGHT = 16;
    private static final int SMILE_X = 68;
    private static final int SMILE_Y = 25;

    private static final int FUEL_SLOT_X = 30;
    private static final int FUEL_SLOT_Y = 28;

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_BORDER = 0xFF555555;
    private static final int COLOR_TRACK = 0xFF1A1A1A;
    private static final int COLOR_BEVEL_LIGHT = 0xFF6B6B6B;
    private static final int COLOR_BEVEL_DARK = 0xFF000000;

    public HellForgeScreen(HellForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 166 + Y_OFFSET;
        this.inventoryLabelY = this.imageHeight - 94;
        // NOT in the constructor: `this.font` is still null here (Screen only sets it once init()
        // runs, right before calling this class's own init()) - centering the title needs it, so
        // this has to happen in init() instead. This is what crashed the whole connection: an NPE
        // inside a ClientboundOpenScreenPacket handler isn't caught anywhere, so it took the
        // network thread down with it ("Network Protocol Error").
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Extra title strip above the furnace texture - filled/bordered by hand so the title sits
        // inside a real panel instead of floating over empty space.
        guiGraphics.fill(x, y, x + this.imageWidth, y + Y_OFFSET, COLOR_PANEL);
        guiGraphics.fill(x, y, x + this.imageWidth, y + 2, COLOR_PANEL_BORDER);
        guiGraphics.fill(x, y, x + 2, y + Y_OFFSET, COLOR_PANEL_BORDER);
        guiGraphics.fill(x + this.imageWidth - 2, y, x + this.imageWidth, y + Y_OFFSET, COLOR_PANEL_BORDER);

        guiGraphics.blit(TEXTURE, x, y + Y_OFFSET, 0, 0, 176, 166);

        renderFuelSlotFrame(guiGraphics, x + FUEL_SLOT_X, y + FUEL_SLOT_Y + Y_OFFSET);
        renderFuelBar(guiGraphics, x + BAR_X, y + BAR_Y);
        renderSmile(guiGraphics, x + SMILE_X, y + SMILE_Y + Y_OFFSET);
    }

    private void renderFuelBar(GuiGraphics guiGraphics, int barLeft, int barTop) {
        guiGraphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, COLOR_TRACK);

        int filled = Math.round(this.menu.getFuelLevel() * BAR_HEIGHT);
        if (filled > 0) {
            // Tiled (not stretched) so the lava texture reads as actual lava rather than one
            // squished-tall image - the scissor rect clips it to just the filled bottom portion,
            // rising and falling with the tank level.
            TextureAtlasSprite lava = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(LAVA_STILL);
            guiGraphics.enableScissor(barLeft, barTop + BAR_HEIGHT - filled, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT);
            for (int tileY = barTop; tileY < barTop + BAR_HEIGHT; tileY += BAR_WIDTH) {
                guiGraphics.blit(barLeft, tileY, 0, BAR_WIDTH, BAR_WIDTH, lava);
            }
            guiGraphics.disableScissor();
        }

        // Tick-mark frame the user supplied, at its native 1:1 pixel size.
        guiGraphics.blit(BAR_OVERLAY, barLeft, barTop, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        // Stylized bevel border around the whole bar: light on top/left, dark on bottom/right.
        guiGraphics.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop, COLOR_BEVEL_LIGHT);
        guiGraphics.fill(barLeft - 1, barTop - 1, barLeft, barTop + BAR_HEIGHT + 1, COLOR_BEVEL_LIGHT);
        guiGraphics.fill(barLeft + BAR_WIDTH, barTop - 1, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1, COLOR_BEVEL_DARK);
        guiGraphics.fill(barLeft - 1, barTop + BAR_HEIGHT, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1, COLOR_BEVEL_DARK);
    }

    /** Reveals the grin left-to-right with cook progress, replacing vanilla's flame/arrow entirely. */
    private void renderSmile(GuiGraphics guiGraphics, int left, int top) {
        int visible = Math.round(this.menu.getCookProgress() * SMILE_WIDTH);
        if (visible <= 0) {
            return;
        }
        guiGraphics.enableScissor(left, top, left + visible, top + SMILE_HEIGHT);
        guiGraphics.blit(SMILE, left, top, 0, 0, SMILE_WIDTH, SMILE_HEIGHT, SMILE_WIDTH, SMILE_HEIGHT);
        guiGraphics.disableScissor();
    }

    /** A stylized 18x18 socket for the fuel slot, which no longer sits over any pre-drawn slot art. */
    private void renderFuelSlotFrame(GuiGraphics guiGraphics, int slotX, int slotY) {
        int left = slotX - 1;
        int top = slotY - 1;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF8B8B8B);
        guiGraphics.fill(left, top, left + 18, top + 1, COLOR_BEVEL_DARK);
        guiGraphics.fill(left, top, left + 1, top + 18, COLOR_BEVEL_DARK);
        guiGraphics.fill(left + 17, top, left + 18, top + 18, COLOR_BEVEL_LIGHT);
        guiGraphics.fill(left, top + 17, left + 18, top + 18, COLOR_BEVEL_LIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int barLeft = this.leftPos + BAR_X;
        int barTop = this.topPos + BAR_Y;
        if (mouseX >= barLeft && mouseX < barLeft + BAR_WIDTH && mouseY >= barTop && mouseY < barTop + BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font,
                    List.of(Component.translatable("gui.merlins_inferno.hell_forge.fuel",
                            this.menu.getStoredFuel(), this.menu.getFuelCapacity())),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
