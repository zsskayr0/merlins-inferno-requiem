package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.compat.OptionalCurios;
import dev.zsskayr.merlins_inferno.item.ChampionSeekerItem;
import dev.zsskayr.merlins_inferno.item.StarSeekerItem;

/**
 * A seeker worn in a Curios slot watches from above the experience level, in the gap between the health and food bars.
 * The stack is drawn as any item, so its pupil turns through the same "angle" property; the slot's eye toggle hides it.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class SeekerHud {
    /** Rests on top of the level number, which starts 35 pixels above the bottom of the screen. */
    private static final int BOTTOM_OFFSET = 52;

    private static long lastScanTick = Long.MIN_VALUE;
    private static ItemStack worn = ItemStack.EMPTY;

    private SeekerHud() {
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "seeker"), (graphics, deltaTracker) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator() || minecraft.options.hideGui) {
            return;
        }
        ItemStack seeker = wornSeeker(player);
        if (!seeker.isEmpty()) {
            graphics.renderItem(seeker, graphics.guiWidth() / 2 - 8, graphics.guiHeight() - BOTTOM_OFFSET);
        }
    }

    /** Looked up once a tick: the Curios bridge is reflective, and this layer runs every frame. */
    private static ItemStack wornSeeker(LocalPlayer player) {
        long tick = player.level().getGameTime();
        if (tick != lastScanTick) {
            lastScanTick = tick;
            worn = OptionalCurios.visible(player).stream()
                    .filter(stack -> stack.getItem() instanceof StarSeekerItem || stack.getItem() instanceof ChampionSeekerItem)
                    .findFirst().orElse(ItemStack.EMPTY);
        }
        return worn;
    }
}
