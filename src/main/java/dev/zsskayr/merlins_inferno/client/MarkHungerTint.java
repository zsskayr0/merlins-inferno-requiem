package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.mark.MarkHelper;

/**
 * A Marked player's hunger bar is drawn with skulls instead of drumsticks, washed out as if the appetite itself were going.
 * It takes the place of the vanilla food layer (same spot, same shaking when starving); nothing in the HUD says why.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class MarkHungerTint {
    private static final ResourceLocation FULL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "hud/mark_food_full");
    private static final ResourceLocation HALF = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "hud/mark_food_half");
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "hud/mark_food_empty");
    private static final float RED = 0.8F;
    private static final float GREEN = 0.74F;
    private static final float BLUE = 0.8F;
    private static final float ALPHA = 0.9F;

    private MarkHungerTint() {
    }

    @SubscribeEvent
    public static void before(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        // A mount's hearts take the food bar's place in vanilla; leave that alone.
        if (player == null || !MarkHelper.hasMark(player) || minecraft.options.hideGui || minecraft.gameMode == null
                || !minecraft.gameMode.canHurtPlayer() || player.getVehicle() instanceof LivingEntity) {
            return;
        }
        event.setCanceled(true);
        draw(event.getGuiGraphics(), minecraft, player.getFoodData());
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, FoodData food) {
        var gui = minecraft.gui;
        int right = graphics.guiWidth() / 2 + 91;
        int top = graphics.guiHeight() - gui.rightHeight;
        gui.rightHeight += 10;
        int level = food.getFoodLevel();
        // Starving (no saturation) makes the icons shake, as in vanilla.
        boolean shake = food.getSaturationLevel() <= 0.0F && gui.getGuiTicks() % (level * 3 + 1) == 0;
        RandomSource random = RandomSource.create(gui.getGuiTicks() * 312871L);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(RED, GREEN, BLUE, ALPHA);
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            int y = top + (shake ? random.nextInt(3) - 1 : 0);
            ResourceLocation sprite = i * 2 + 1 < level ? FULL : i * 2 + 1 == level ? HALF : EMPTY;
            graphics.blitSprite(sprite, x, y, 9, 9);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** Whatever happened to the layer, the next thing drawn is never tinted. */
    @SubscribeEvent
    public static void afterHud(RenderGuiEvent.Post event) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
