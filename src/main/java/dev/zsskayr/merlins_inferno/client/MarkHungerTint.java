package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.mark.MarkHelper;

/**
 * A Marked player's hunger bar is washed out: the icons are drawn dimmed and greyed, as if the appetite itself were
 * going. Only the bar is touched; nothing in the HUD says why.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class MarkHungerTint {
    private static final float RED = 0.74F;
    private static final float GREEN = 0.68F;
    private static final float BLUE = 0.74F;
    private static final float ALPHA = 0.88F;

    private MarkHungerTint() {
    }

    @SubscribeEvent
    public static void before(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
            var player = Minecraft.getInstance().player;
            if (player != null && MarkHelper.hasMark(player)) {
                RenderSystem.setShaderColor(RED, GREEN, BLUE, ALPHA);
            }
        }
    }

    @SubscribeEvent
    public static void after(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /** Whatever happened to the layer, the next thing drawn is never tinted. */
    @SubscribeEvent
    public static void afterHud(RenderGuiEvent.Post event) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
