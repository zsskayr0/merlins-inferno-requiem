package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.ZaeldrisItem;
import dev.zsskayr.merlins_inferno.network.ZaeldrisBarragePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Zaeldris' barrage key, right mouse by default. It is its own mapping and not the Use key, so it keeps working under Epic
 * Fight (whose guard also listens on right mouse) and can be moved anywhere. Only the key going down and up is sent.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class ZaeldrisKeys {
    public static final KeyMapping BARRAGE = new KeyMapping("key.merlins_inferno.zaeldris_barrage", KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.merlins_inferno");
    private static boolean sent;
    /** Client ticks the barrage has been held, for the first-person draw-back in ZaeldrisPose. */
    private static int heldTicks;
    private ZaeldrisKeys() {}

    /** True while this client holds the barrage with the lance in hand. */
    public static boolean isBarraging() {
        return sent;
    }

    public static float heldTicks(float partialTick) {
        return sent ? heldTicks + partialTick : 0.0F;
    }

    @SubscribeEvent static void register(RegisterKeyMappingsEvent event) { event.register(BARRAGE); }

    @SubscribeEvent static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            sent = false;
            return;
        }
        boolean ready = minecraft.screen == null && player.getMainHandItem().getItem() instanceof ZaeldrisItem;
        boolean tapped = false;
        while (BARRAGE.consumeClick()) {
            tapped = true;
        }
        boolean held = BARRAGE.isDown() && ready;
        // A click that went down and up between two ticks never shows as held: send it as a press and a release.
        if (tapped && ready && !held && !sent) {
            PacketDistributor.sendToServer(new ZaeldrisBarragePayload(true));
            PacketDistributor.sendToServer(new ZaeldrisBarragePayload(false));
            return;
        }
        heldTicks = held && sent ? heldTicks + 1 : 0;
        if (held != sent) {
            sent = held;
            PacketDistributor.sendToServer(new ZaeldrisBarragePayload(held));
        }
    }
}
