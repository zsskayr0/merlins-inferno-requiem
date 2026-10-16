package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.shaders.FogShape;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid=Merlins_inferno.MODID,value=Dist.CLIENT)
public final class LavaSight {
    private static boolean active() {
        var mc=Minecraft.getInstance();
        return mc.player!=null && mc.player.hasEffect(ModEffects.LAVA_SIGHT)
                && mc.gameRenderer.getMainCamera().getFluidInCamera()==FogType.LAVA;
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        if (event.getType()!=FogType.LAVA || !active()) return;
        event.setNearPlaneDistance(.25F);
        event.setFarPlaneDistance(24);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event) {
        if (!active()) return;
        event.setRed(.24F);event.setGreen(.085F);event.setBlue(.025F);
    }
    @SubscribeEvent public static void overlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType()==RenderBlockScreenEffectEvent.OverlayType.FIRE && active()) event.setCanceled(true);
    }
}
