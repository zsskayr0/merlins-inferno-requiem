package dev.zsskayr.merlins_inferno.client;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import dev.zsskayr.merlins_inferno.menu.PandoraBoxMenu;
import dev.zsskayr.merlins_inferno.menu.PandoraLayout;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Recipe;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Pandora's layered GUI. Animated visuals and hit testing share the same orbit coordinates. */
public class PandoraBoxScreen extends AbstractContainerScreen<PandoraBoxMenu> {
    private static final ResourceLocation BACKGROUND = texture("pandora_background.png");
    private static final ResourceLocation SLOTS = texture("hell_forge_old.png");
    private static final int TEXT = 0xDFDDE0;
    private final ItemStack[] confirmationItems = new ItemStack[4];
    private Button ritualButton;
    private final PandoraAnimation animation = new PandoraAnimation();
    private float guiScale = 1;
    private int confirmationTicks;
    private boolean armed, pointerDown;

    public PandoraBoxScreen(PandoraBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PandoraLayout.WIDTH;
        imageHeight = PandoraLayout.HEIGHT;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("merlins_inferno", "textures/gui/pandora/" + name);
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.merlins_inferno.pandora_box." + key, args);
    }

    @Override
    protected void init() {
        super.init();
        guiScale = PandoraLayout.scale(width, height);
        armed = false;
        pointerDown = false;
        ritualButton = addRenderableWidget(new ForgeButton(leftPos + PandoraLayout.BUTTON_X,
                topPos + PandoraLayout.BUTTON_Y, PandoraLayout.BUTTON_WIDTH, PandoraLayout.BUTTON_HEIGHT,
                text("forge_short"), button -> onRitualClick()));
        updateButtons();
    }

    private void onRitualClick() {
        if (!menu.canForge() || minecraft.gameMode == null) {
            armed = false;
            updateButtons();
            return;
        }
        if (menu.getMatchedRecipe() == Recipe.AWAKENING && (!armed || !confirmationMatches())) {
            armed = true;
            confirmationTicks = 100;
            for (int i = 0; i < 4; i++) confirmationItems[i] = menu.getSlot(i).getItem().copy();
            updateButtons();
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                PandoraBoxMenu.BUTTON_FORGE);
        armed = false;
        updateButtons();
    }

    private boolean confirmationMatches() {
        for (int i = 0; i < 4; i++) {
            if (confirmationItems[i] == null || !ItemStack.matches(confirmationItems[i], menu.getSlot(i).getItem())) return false;
        }
        return true;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (armed && (--confirmationTicks <= 0 || menu.getMatchedRecipe() != Recipe.AWAKENING || !menu.canForge() || !confirmationMatches())) armed = false;
        updateButtons();
    }

    private void updateButtons() {
        ritualButton.active = menu.canForge();
        ritualButton.setMessage(text(armed ? "confirm_forge" : "forge_short"));
    }

    private double logicalX(double x) { return PandoraLayout.unscale(x, width, guiScale); }
    private double logicalY(double y) { return PandoraLayout.unscale(y, height, guiScale); }
    private float slotX(int index) { return animation.slotX(index); }
    private float slotY(int index) { return animation.slotY(index); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int mx = (int) Math.floor(logicalX(mouseX)), my = (int) Math.floor(logicalY(mouseY));
        boolean nearOrbit = mx >= leftPos + 54 && mx < leftPos + 178 && my >= topPos + 24 && my < topPos + 132;
        animation.update(Util.getMillis(), menu.isCrafting(), menu.getCraftProgress(), menu.getCompletionSerial(),
                nearOrbit || pointerDown || isQuickCrafting || !menu.getCarried().isEmpty() || armed,
                minecraft.options.touchscreen().get());
        renderTransparentBackground(graphics);
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F, 0);
        graphics.pose().scale(guiScale, guiScale, 1);
        graphics.pose().translate(-width / 2.0F, -height / 2.0F, 0);
        super.render(graphics, mx, my, partialTick);
        renderTooltip(graphics, mx, my);
        renderHelp(graphics, mx, my);
        graphics.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    // Route every pointer interaction through the same transform used for rendering, including quick crafting.
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        pointerDown = true;
        return super.mouseClicked(logicalX(x), logicalY(y), button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        boolean result = super.mouseReleased(logicalX(x), logicalY(y), button);
        pointerDown = false;
        return result;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(logicalX(x), logicalY(y), button, dx / guiScale, dy / guiScale);
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(logicalX(x), logicalY(y));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return super.mouseScrolled(logicalX(x), logicalY(y), horizontal, vertical);
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        // Vanilla findSlot, click, shift-click, drag and hover all reach this method.
        for (int i = 0; i < 3; i++) {
            Slot slot = menu.getSlot(i);
            if (w == 16 && h == 16 && x == slot.x && y == slot.y) {
                return PandoraLayout.offeringHovered(slotX(i), slotY(i), mouseX - leftPos, mouseY - topPos, menu.hasResult());
            }
        }
        return super.isHovering(x, y, w, h, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        graphics.pose().pushPose();
        if (slot.index < 3) graphics.pose().translate(slotX(slot.index) - slot.x, slotY(slot.index) - slot.y, 0);
        super.renderSlot(graphics, slot);
        graphics.pose().popPose();
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        if (slot.index < 3) graphics.pose().translate(slotX(slot.index) - slot.x, slotY(slot.index) - slot.y, 0);
        super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        RenderSystem.enableBlend();
        graphics.blit(BACKGROUND, x, y, imageWidth, imageHeight, 0, 0, 1536, 1024, 1536, 1024);
        // Reuse the original empty left panel as a continuous interior. No baked-in sidebar remains.
        graphics.blit(BACKGROUND, x + 4, y + 22, imageWidth - 8, 137, 24, 96, 940, 532, 1536, 1024);
        for (int step = 0; step < 160; step++) {
            double a = step * Math.PI / 80;
            float px = x + PandoraLayout.CENTER_X + (float) (40 * Math.cos(a));
            float py = y + PandoraLayout.CENTER_Y + (float) (40 * Math.sin(a));
            pixel(graphics, px, py, 0.75F, step % 10 < 2 ? 0xFF735145 : 0xFF49464D);
        }
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            if (slot.index == 3 || slot.index == PandoraBoxMenu.RESULT_SLOT) {
                frame(graphics, x + slot.x - 7, y + slot.y - 7, 30, 30, menu.hasResult());
            } else {
                graphics.pose().pushPose();
                if (slot.index < 3) graphics.pose().translate(slotX(slot.index) - slot.x, slotY(slot.index) - slot.y, 0);
                graphics.blit(SLOTS, x + slot.x - 1, y + slot.y - 1, 7, 83, 18, 18, 256, 256);
                graphics.pose().popPose();
            }
        }
        renderRitualLight(graphics, x, y);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        fit(graphics, title, 42, 7, imageWidth - 84, TEXT);
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, ItemStack stack, Slot slot, String count) {
        // A forging animation represents the ingredient, not the whole surplus stack's counter.
        if (menu.isCrafting() && slot.index < 4) graphics.renderItem(stack, slot.x, slot.y);
        else super.renderSlotContents(graphics, stack, slot, count);
    }

    private static void pixel(GuiGraphics graphics, float x, float y, float size, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(size, size, 1);
        graphics.fill(0, 0, 1, 1, color);
        graphics.pose().popPose();
    }

    private void renderRitualLight(GuiGraphics graphics, int x, int y) {
        float progress = animation.progress(), flash = animation.flash();
        float power = menu.isCrafting() ? progress : flash;
        if (power <= 0) return;
        float cx = x + PandoraLayout.CENTER_X, cy = y + PandoraLayout.CENTER_Y;
        // Translucent layered discs, smooth moving sparks and short trails, all sampled each frame.
        for (int radius = 28; radius >= 8; radius -= 3) {
            int alpha = (int) ((flash > 0 ? 25 : 12) * power);
            for (int dy = -radius; dy <= radius; dy++) {
                int span = (int) Math.sqrt(radius * radius - dy * dy);
                graphics.fill((int) cx - span, (int) cy + dy, (int) cx + span + 1, (int) cy + dy + 1, (alpha << 24) | 0xFFC078);
            }
        }
        if (menu.isCrafting()) {
            for (int i = 0; i < 3; i++) {
                if (!menu.getSlot(i).hasItem()) continue;
                for (int trail = 1; trail <= 12; trail++) {
                    double a = animation.angle() + i * Math.PI * 2 / 3 - trail * 0.055;
                    float radius = animation.radius() + trail * progress * 0.25F;
                    int alpha = (int) ((1 - trail / 13.0F) * power * 150);
                    pixel(graphics, cx + (float) Math.cos(a) * radius, cy + (float) Math.sin(a) * radius,
                            1.5F, (alpha << 24) | 0xFFAE66);
                }
            }
        }
        for (int i = 0; i < 24; i++) {
            double a = animation.angle() + i * Math.PI * 2 / 24;
            double radius = flash > 0 ? (1 - flash) * 45 : 40 * (1 - ((progress * 2 + i / 24.0) % 1));
            pixel(graphics, cx + (float) (Math.cos(a) * radius), cy + (float) (Math.sin(a) * radius),
                    1.2F + power * 0.6F, ((int) (210 * power) << 24) | 0xFFE4B3);
        }
    }

    private void renderHelp(GuiGraphics graphics, int mx, int my) {
        if (!menu.getCarried().isEmpty() || menu.isCrafting()) return;
        if (armed) {
            boolean spared = minecraft.player != null && (minecraft.player.isCreative() || minecraft.level.getLevelData().isHardcore());
            graphics.renderTooltip(font, text(spared ? "warning_spared" : "warning"), mx, my);
        } else if (hoveredSlot != null && hoveredSlot.index < 4 && !hoveredSlot.hasItem()) {
            graphics.renderTooltip(font, text(hoveredSlot.index == 3 ? "catalyst" : "offering"), mx, my);
        } else if (ritualButton.isHoveredOrFocused()) {
            ItemStack result = PandoraBoxMenu.result(menu.getMatchedRecipe());
            if (menu.hasResult()) graphics.renderTooltip(font, text("take_result"), mx, my);
            else if (!result.isEmpty()) graphics.renderTooltip(font, result, mx, my);
        }
    }

    private void fit(GuiGraphics graphics, Component label, int x, int y, int maxWidth, int color) {
        float scale = Math.min(1, maxWidth / (float) Math.max(1, font.width(label)));
        graphics.pose().pushPose();
        graphics.pose().translate(x + maxWidth / 2.0F, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, label, -font.width(label) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private static void frame(GuiGraphics graphics, int x, int y, int w, int h, boolean selected) {
        graphics.fill(x, y, x + w, y + h, 0xFF17171B);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, selected ? 0xFFBBA992 : 0xFF87868C);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF33343B);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFF9A5143);
        graphics.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF321D21);
    }

    private class ForgeButton extends Button {
        ForgeButton(int x, int y, int w, int h, Component label, OnPress action) {
            super(x, y, w, h, label, action, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            frame(graphics, getX(), getY(), getWidth(), getHeight(), active && isHoveredOrFocused());
            float progress = animation.flash() > 0 ? 1 : animation.progress();
            if (progress > 0) {
                graphics.pose().pushPose();
                graphics.pose().translate(getX() + 4, getY() + 4, 0);
                graphics.pose().scale((getWidth() - 8) * progress, 1, 1);
                graphics.fillGradient(0, 0, 1, getHeight() - 8, 0xFFE1A061, 0xFF923F28);
                graphics.pose().popPose();
                pixel(graphics, getX() + 4 + (getWidth() - 8) * progress - 1, getY() + 4, 1, 0xFFFFD7A2);
            }
            fit(graphics, getMessage(), getX() + 3, getY() + (getHeight() - 8) / 2, getWidth() - 6,
                    active || menu.isCrafting() ? TEXT : 0x79737B);
        }
    }
}
