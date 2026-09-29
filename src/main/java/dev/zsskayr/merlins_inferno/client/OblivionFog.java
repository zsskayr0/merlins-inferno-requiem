package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;

/** Oblivion's limbo mist: the terrain fades into the pale fog close by, so the void reads as soft rather than empty. */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class OblivionFog {
    private static final float NEAR = 6.0F;
    private static final float FAR = 110.0F;

    private OblivionFog() {
    }

    @SubscribeEvent
    static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.dimension() != OblivionDimension.LEVEL
                || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        event.setNearPlaneDistance(Math.min(NEAR, event.getFarPlaneDistance()));
        event.setFarPlaneDistance(Math.min(FAR, event.getFarPlaneDistance()));
        event.setCanceled(true);
    }
}
