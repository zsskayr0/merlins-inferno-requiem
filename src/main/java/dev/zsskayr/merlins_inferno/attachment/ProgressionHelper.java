package dev.zsskayr.merlins_inferno.attachment;

import net.minecraft.world.entity.player.Player;

import dev.zsskayr.merlins_inferno.registry.ModAttachments;

/**
 * A player's Circle (see the progression design): 1 until the Pandora Box ritual is performed, 2 after it
 * (Circle 3 is not implemented yet). Stored as a serialized, copy-on-death player attachment.
 */
public final class ProgressionHelper {
    public static final int FIRST_CIRCLE = 1;
    public static final int SECOND_CIRCLE = 2;

    private ProgressionHelper() {
    }

    public static int circle(Player player) {
        return player.getData(ModAttachments.CIRCLE);
    }

    public static void setCircle(Player player, int circle) {
        player.setData(ModAttachments.CIRCLE, circle);
    }

    public static boolean hasReached(Player player, int circle) {
        return circle(player) >= circle;
    }
}
