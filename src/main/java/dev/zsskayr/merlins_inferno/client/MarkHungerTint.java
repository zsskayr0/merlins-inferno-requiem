package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.mark.MarkHelper;

/**
 * A Marked player's hunger bar is drawn with skulls instead of drumsticks, washed out as if the appetite itself were going.
 * It sits exactly where the vanilla food layer draws (same spot, same shaking when starving); nothing in the HUD says why.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class MarkHungerTint {
    private static final ResourceLocation FULL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "hud/mark_food_full");
    private static final ResourceLocation HALF = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "hud/mark_food_half");
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "hud/mark_food_empty");
    private static final float RED = 0.8F;
    private static final float GREEN = 0.74F;
    private static final float BLUE = 0.8F;
    private static final float HUNGRY_RED = 0.62F;
    private static final float HUNGRY_GREEN = 0.8F;
    private static final float HUNGRY_BLUE = 0.5F;
    private static final float ALPHA = 0.9F;

    private MarkHungerTint() {
    }

    /** Marked, and nothing else is standing in for the food bar (a mount's hearts take its place in vanilla). */
    private static boolean marked(Minecraft minecraft) {
        var player = minecraft.player;
        return player != null && MarkHelper.hasMark(player) && !minecraft.options.hideGui && minecraft.gameMode != null
                && minecraft.gameMode.canHurtPlayer() && !(player.getVehicle() instanceof LivingEntity);
    }

    /**
     * The vanilla layer is let run - so HUD mods hiding or cancelling the bar still do, and take the skulls with it - but its
     * drumsticks are drawn fully transparent. Mods that decorate the bar afterwards (AppleSkin's saturation and restore
     * overlays) see the bar where it always is and draw on top of the skulls.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void before(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL) && marked(Minecraft.getInstance())) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.0F);
        }
    }

    /** Runs before the other mods' Post handlers, so whatever they draw over the bar lands on the skulls. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void after(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft minecraft = Minecraft.getInstance();
            if (marked(minecraft)) {
                draw(event.getGuiGraphics(), minecraft, minecraft.player.getFoodData(), minecraft.player.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER));
            }
        }
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, FoodData food, boolean hungry) {
        var gui = minecraft.gui;
        int right = graphics.guiWidth() / 2 + 91;
        // The vanilla layer has already claimed its row: the bar is the one just below the current height.
        int top = graphics.guiHeight() - (gui.rightHeight - 10);
        int level = food.getFoodLevel();
        // Starving (no saturation) makes the icons shake, as in vanilla.
        boolean shake = food.getSaturationLevel() <= 0.0F && gui.getGuiTicks() % (level * 3 + 1) == 0;
        RandomSource random = RandomSource.create(gui.getGuiTicks() * 312871L);
        RenderSystem.enableBlend();
        // The Hunger effect sickens it, as it does the vanilla drumsticks.
        if (hungry) {
            RenderSystem.setShaderColor(HUNGRY_RED, HUNGRY_GREEN, HUNGRY_BLUE, ALPHA);
        } else {
            RenderSystem.setShaderColor(RED, GREEN, BLUE, ALPHA);
        }
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            int y = top + (shake ? random.nextInt(3) - 1 : 0);
            ResourceLocation sprite = i * 2 + 1 < level ? FULL : i * 2 + 1 == level ? HALF : EMPTY;
            graphics.blitSprite(sprite, x + 1, y, 8, 9);
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
