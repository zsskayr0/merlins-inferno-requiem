package dev.zsskayr.merlins_inferno.client.elysium;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.block.CloudSeaBlock;
import dev.zsskayr.merlins_inferno.client.IrisFogCompat;

/**
 * Client hooks for El'ysium: registers {@link ElysiumSkyEffects}, keeps the cloud sea visible when the player has turned
 * clouds off (vanilla then never calls {@code renderClouds}: it is the floor of the world, not weather), and hands the
 * job to the physical cloud blocks while a shader pack runs. Packs replace or discard anything drawn outside the terrain
 * pipeline, so under a pack {@link ElysiumCloudSea} stays silent and {@link CloudSeaBlock} shows instead.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class ElysiumClientEvents {
    private ElysiumClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ElysiumSkyEffects.ID, new ElysiumSkyEffects());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level != null && mc.level.effects() instanceof ElysiumSkyEffects)) {
            return;
        }
        boolean pack = IrisFogCompat.irisReady();
        if (pack != CloudSeaBlock.isShown()) {
            CloudSeaBlock.setShown(pack);
            mc.levelRenderer.allChanged(); // re-mesh: the render shape of the cloud blocks just changed
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !(level.effects() instanceof ElysiumSkyEffects) || mc.options.getCloudsType() != CloudStatus.OFF) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        ElysiumCloudSea.render(level, event.getRenderTick() + partialTick, partialTick, event.getModelViewMatrix(),
                event.getProjectionMatrix(), cam.x, cam.y, cam.z);
    }
}
