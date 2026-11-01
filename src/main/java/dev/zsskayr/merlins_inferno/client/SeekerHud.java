package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
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
    /** Where the selected item's name has to start to clear the seeker, still below the action bar. */
    private static final int ITEM_NAME_BOTTOM_OFFSET = 62;

    private static long lastScanTick = Long.MIN_VALUE;
    private static ItemStack worn = ItemStack.EMPTY;
    private static boolean itemNameRaised;

    private SeekerHud() {
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "seeker"), (graphics, deltaTracker) -> render(graphics));
    }

    /** Last in line, so nothing cancels the layer after this and its Post, which undoes the lift, always fires. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void beforeItemName(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME) || shownSeeker(minecraft).isEmpty()) {
            return;
        }
        // Gui.renderSelectedItemName's position: above the bars, and lower for a player who takes no damage.
        int nameOffset = Math.max(Math.max(minecraft.gui.leftHeight, minecraft.gui.rightHeight), 59) - (minecraft.gameMode.canHurtPlayer() ? 0 : 14);
        int lift = ITEM_NAME_BOTTOM_OFFSET - nameOffset;
        if (lift > 0) {
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0.0F, -lift, 0.0F);
            itemNameRaised = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void afterItemName(RenderGuiLayerEvent.Post event) {
        if (itemNameRaised && event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            event.getGuiGraphics().pose().popPose();
            itemNameRaised = false;
        }
    }

    private static void render(GuiGraphics graphics) {
        ItemStack seeker = shownSeeker(Minecraft.getInstance());
        if (!seeker.isEmpty()) {
            graphics.renderItem(seeker, graphics.guiWidth() / 2 - 8, graphics.guiHeight() - BOTTOM_OFFSET);
        }
    }

    private static ItemStack shownSeeker(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        return player == null || player.isSpectator() || minecraft.options.hideGui ? ItemStack.EMPTY : wornSeeker(player);
    }

    /** Looked up once a tick: the Curios bridge is reflective, and this runs every frame. */
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
