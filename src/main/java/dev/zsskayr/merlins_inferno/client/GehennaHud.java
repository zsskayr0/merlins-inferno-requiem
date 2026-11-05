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
import dev.zsskayr.merlins_inferno.event.GehennaHandler;

/**
 * Gehenna's clock, left of the hotbar (past the offhand slot): a blood-red gothic dial whose hand sweeps the frenzy's
 * six seconds from twelve o'clock, and beats faster in the last two; the fed boon shows as blood drops above it.
 * Hidden while neither runs.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class GehennaHud {
    private static final ResourceLocation DIAL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/gui/gehenna_clock.png");
    private static final ResourceLocation HAND = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/gui/gehenna_clock_hand.png");
    private static final int SIZE = 24;
    private static final int FRENZY_TICKS = GehennaHandler.FRENZY_SECONDS * 20;

    private static int fed;
    private static int frenzyTicks;
    private static long receivedAt;

    private GehennaHud() {
    }

    public static void set(int fedBlows, int ticksLeft) {
        fed = fedBlows;
        frenzyTicks = ticksLeft;
        receivedAt = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        fed = 0;
        frenzyTicks = 0;
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "gehenna_clock"), GehennaHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || minecraft.player.isSpectator()) {
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float left = frenzyTicks - (minecraft.level.getGameTime() - receivedAt) - partial;
        boolean frenzy = frenzyTicks > 0 && left > 0;
        if (!frenzy && fed <= 0) {
            return;
        }
        // left of the hotbar, and of the offhand slot when it sits on that side
        int x = graphics.guiWidth() / 2 - 91 - SIZE - 6;
        if (minecraft.player.getMainArm() == HumanoidArm.RIGHT && !minecraft.player.getOffhandItem().isEmpty()) {
            x -= 29;    // only make way for the offhand slot when it is drawn
        }
        int y = graphics.guiHeight() - SIZE;

        if (!frenzy) {
            // only the fed boon: just its drops of blood, no clock - nothing is counting down
            drops(graphics, x, y + SIZE - 6);
            return;
        }
        float glow = 1.0F;
        if (frenzy && left < 40) {
            // the heartbeat quickens toward the toll
            glow = 0.65F + 0.35F * (0.5F + 0.5F * Mth.sin((FRENZY_TICKS - left) * (0.35F + (40 - left) * 0.012F)));
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(glow, glow, glow, 1.0F);
        graphics.blit(DIAL, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float angle = frenzy ? (1.0F - left / FRENZY_TICKS) * 360.0F : 0.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + SIZE / 2.0F, y + SIZE / 2.0F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.blit(HAND, -SIZE / 2, -SIZE / 2, 0, 0, SIZE, SIZE, SIZE, SIZE);
        graphics.pose().popPose();

        drops(graphics, x, y);
        RenderSystem.disableBlend();
    }

    /** The fed boon: a drop of blood per blow left. */
    private static void drops(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < fed; i++) {
            int dx = x + SIZE / 2 - 5 + i * 4;
            graphics.fill(dx, y - 4, dx + 2, y - 1, 0xFF7A1726);
            graphics.fill(dx, y - 5, dx + 2, y - 4, 0xFFC43C40);
        }
    }
}
