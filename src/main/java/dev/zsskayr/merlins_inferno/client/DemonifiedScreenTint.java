package dev.zsskayr.merlins_inferno.client;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** A translucent GUI layer avoids replacing the world renderer's shader pipeline. */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class DemonifiedScreenTint {
    private DemonifiedScreenTint() {}

    @SubscribeEvent public static void onGui(RenderGuiEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var effect = player.getEffect(ModEffects.DEMONIFIED);
        if (effect == null) return;
        int alpha = Math.min(38, 18 + effect.getAmplifier() * 4);
        var graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xAB1520);
    }
}
