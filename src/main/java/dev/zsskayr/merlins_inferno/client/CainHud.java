package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.event.MarkOfCainHandler;

/**
 * The Mark of Cain's clock, right of the hotbar (Gehenna's sits on the left): the same blood-red gothic dial with seven
 * spikes, whose hand sweeps the time to the next obligatory kill. Overdue, the hand rests at the top and the dial
 * throbs dark; with the Mark of the Murder armed, the pivot burns. Shown only to a bearer.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class CainHud {
    private static final ResourceLocation DIAL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/gui/cain_clock.png");
    private static final ResourceLocation HAND = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/gui/cain_clock_hand.png");
    private static final int SIZE = 24;

    private static boolean bearing;
    private static int ticksLeft;
    private static boolean armed;
    private static long receivedAt;

    private CainHud() {
    }

    public static void set(boolean bears, int left, boolean murderArmed) {
        bearing = bears;
        ticksLeft = left;
        armed = murderArmed;
        receivedAt = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        bearing = false;
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "cain_clock"), CainHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!bearing || minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || minecraft.player.isSpectator()) {
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float left = ticksLeft - (minecraft.level.getGameTime() - receivedAt) - partial;
        boolean overdue = left <= 0;
        // right of the hotbar, and of the offhand slot when a left-handed player has it on that side
        int x = graphics.guiWidth() / 2 + 91 + 6;
        if (minecraft.player.getMainArm() == HumanoidArm.LEFT && !minecraft.player.getOffhandItem().isEmpty()) {
            x += 29;
        }
        int y = graphics.guiHeight() - SIZE;

        float shade = 1.0F;
        if (overdue) {
            shade = 0.45F + 0.25F * (0.5F + 0.5F * Mth.sin((minecraft.level.getGameTime() + partial) * 0.25F));
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(shade, shade, shade, 1.0F);
        graphics.blit(DIAL, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float angle = overdue ? 0.0F : (1.0F - left / MarkOfCainHandler.KILL_WINDOW) * 360.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + SIZE / 2.0F, y + SIZE / 2.0F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.blit(HAND, -SIZE / 2, -SIZE / 2, 0, 0, SIZE, SIZE, SIZE, SIZE);
        graphics.pose().popPose();

        if (armed) {
            // the Murder waits: the pivot burns
            float beat = 0.5F + 0.5F * Mth.sin((minecraft.level.getGameTime() + partial) * 0.3F);
            int alpha = (int) (150 + 105 * beat);
            graphics.fill(x + 11, y + 11, x + 13, y + 13, (alpha << 24) | 0xFF5A4A);
        }
        RenderSystem.disableBlend();
    }
}
