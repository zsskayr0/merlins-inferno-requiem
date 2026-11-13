package dev.zsskayr.merlins_inferno.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;

/**
 * The Hellforge's pool is drawn, not placed: no fluid blocks exist. This gives it the feel of one for the local player -
 * lava-like drag, a slow swim upwards while jump is held, no fall damage - and the thick, coloured view of being under it.
 * The burning itself is done on the server (see {@code HellForgeBlockEntity#scaldPool}).
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class HellForgePoolClient {
    private static final Set<HellForgeBlockEntity> FORGES = Collections.newSetFromMap(new WeakHashMap<>());

    private HellForgePoolClient() {
    }

    private static AABB pool(HellForgeBlockEntity forge) {
        return forge.isRemoved() ? null : forge.poolBox(forge.renderFill() / 128.0F);
    }

    public static void tick(HellForgeBlockEntity forge) {
        FORGES.add(forge);
        var player = Minecraft.getInstance().player;
        AABB pool = pool(forge);
        if (player == null || pool == null || !player.getBoundingBox().intersects(pool)) return;
        player.resetFallDistance();
        // Like lava: thick, so every motion is halved, and most of gravity is held back (vanilla lava keeps a quarter of it).
        // You sink slowly, never fast, and holding jump swims you up.
        // You float at the surface; in a liquid you can sink by sneaking, but coal's bed of embers holds you up.
        Vec3 motion = player.getDeltaMovement().multiply(0.5, 0.5, 0.5);
        var fuel = forge.renderFuel();
        boolean sinkable = fuel == null || fuel.sinkable();
        var options = Minecraft.getInstance().options;
        double lift = options.keyJump.isDown() ? 0.1 : sinkable && options.keyShift.isDown() ? 0.0 : 0.06;
        player.setDeltaMovement(motion.x, Mth.clamp(motion.y + lift, -0.08, 0.2), motion.z);
    }

    /** The pool the camera is in, or null. */
    private static HellForgeBlockEntity cameraPool() {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (HellForgeBlockEntity forge : FORGES) {
            AABB pool = pool(forge);
            if (pool != null && pool.contains(camera)) return forge;
        }
        return null;
    }

    /**
     * Shader packs draw their own fog and ignore the vanilla fog settings, so being under the surface is also shown as a
     * thick wash of the fuel's colour over the screen, like the vanilla view from inside lava.
     */
    @SubscribeEvent
    static void onOverlay(net.neoforged.neoforge.client.event.RenderGuiEvent.Pre event) {
        HellForgeBlockEntity forge = cameraPool();
        if (forge == null || Minecraft.getInstance().options.getCameraType() != net.minecraft.client.CameraType.FIRST_PERSON) return;
        var fuel = forge.renderFuel();
        int color = fuel == null ? 0xF48C24 : fuel.uiColor();
        var g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        int dark = (color >> 17 & 127) << 16 | (color >> 9 & 127) << 8 | (color >> 1 & 127);
        g.fill(0, 0, w, h, 0xC0000000 | dark);
        g.fillGradient(0, 0, w, h / 3, 0x60000000, 0x00000000);
        g.fillGradient(0, h * 2 / 3, w, h, 0x00000000, 0x60000000);
    }

    @SubscribeEvent
    static void onFog(ViewportEvent.RenderFog event) {
        if (cameraPool() == null) return;
        event.setNearPlaneDistance(0.25F);
        event.setFarPlaneDistance(2.5F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onFogColor(ViewportEvent.ComputeFogColor event) {
        HellForgeBlockEntity forge = cameraPool();
        if (forge == null) return;
        var fuel = forge.renderFuel();
        int color = fuel == null ? 0xF48C24 : fuel.uiColor();
        event.setRed((color >> 16 & 255) / 255F * 0.8F);
        event.setGreen((color >> 8 & 255) / 255F * 0.8F);
        event.setBlue((color & 255) / 255F * 0.8F);
    }
}
