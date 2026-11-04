package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.ExcaliburAwakening;

/**
 * Client side of Excalibur's awakening: the earthquake in the camera of everyone near it (growing through the call,
 * a jolt at the climax), the white flash at the climax, and the frame the floating sword shows (worn to true).
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class ExcaliburAwakeningClient {
    /** Beyond this distance the quake is not felt. */
    private static final double FELT_WITHIN = 40.0;
    private static final int FLASH_TICKS = 14;
    private static final RandomSource RANDOM = RandomSource.create();

    /** Quake strength this tick (0 none, 1 the climax up close); set by the rituals in range, read by the camera. */
    private static float quake;
    private static float quakeNext;
    private static int flash;
    /** The awakening sword's frame while the renderer draws it; outside that, the model shows the true sword. */
    private static float morph = 1.0F;

    private ExcaliburAwakeningClient() {
    }

    /** Called from the ritual entity's client tick. */
    public static void tick(ExcaliburAwakening ritual) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        double distance = player.distanceTo(ritual);
        if (distance > FELT_WITHIN) {
            return;
        }
        int t = ritual.tickCount;
        float phase;
        if (t < ExcaliburAwakening.CLIMAX_AT) {
            phase = 0.15F + 0.55F * (float) Math.pow((float) t / ExcaliburAwakening.CLIMAX_AT, 1.5);
        } else {
            phase = Math.max(0.0F, 1.0F - (t - ExcaliburAwakening.CLIMAX_AT) / 14.0F);
        }
        float near = (float) (1.0 - distance / FELT_WITHIN);
        quakeNext = Math.max(quakeNext, phase * near);
        if (t == ExcaliburAwakening.CLIMAX_AT && distance < FELT_WITHIN) {
            flash = FLASH_TICKS;
        }
    }

    public static void setMorph(float value) {
        morph = value;
    }

    public static void clearMorph() {
        morph = 1.0F;
    }

    /** What the awakening sword's model override reads (models/item/excalibur_awakening.json). */
    public static float morph() {
        return morph;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        quake = quakeNext;
        quakeNext = 0.0F;
        if (flash > 0) {
            flash--;
        }
    }

    @SubscribeEvent
    static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (quake <= 0.0F || Minecraft.getInstance().isPaused()) {
            return;
        }
        float s = quake * quake;
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * 3.0F * s);
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * 3.0F * s);
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * 5.0F * s);
    }

    @SubscribeEvent
    static void onGui(RenderGuiEvent.Post event) {
        if (flash <= 0) {
            return;
        }
        float alpha = Mth.clamp((flash - event.getPartialTick().getGameTimeDeltaPartialTick(false)) / FLASH_TICKS, 0.0F, 1.0F) * 0.9F;
        var graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ((int) (alpha * 255) << 24) | 0xFFF8E6);
    }
}
