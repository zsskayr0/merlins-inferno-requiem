package dev.zsskayr.merlins_inferno.attachment;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import dev.zsskayr.merlins_inferno.network.CirclePayload;

/**
 * The world's Circle: 1 until the Pandora Box ritual is performed by anyone, 2 after it; 3 is a valid value that does
 * nothing yet. It only goes up, except through {@link #forceCircle} (debug). The value lives on the server in
 * {@link WorldCircle}; clients keep a synced copy that {@link #circle(Player)} reads on the logical client.
 */
public final class ProgressionHelper {
    public static final int FIRST_CIRCLE = 1;
    public static final int SECOND_CIRCLE = 2;
    public static final int THIRD_CIRCLE = 3;

    /** The client's copy of the world's Circle, set by {@link CirclePayload}. */
    private static volatile int clientCircle = FIRST_CIRCLE;

    private ProgressionHelper() {
    }

    static int clamp(int circle) {
        return Math.max(FIRST_CIRCLE, Math.min(THIRD_CIRCLE, circle));
    }

    public static int circle(MinecraftServer server) {
        return WorldCircle.get(server).circle();
    }

    public static int circle(ServerLevel level) {
        return circle(level.getServer());
    }

    /** Works on either side: the server reads the world data, the client its synced copy. */
    public static int circle(Player player) {
        return player.level().isClientSide ? clientCircle : circle(player.level().getServer());
    }

    public static boolean hasReached(ServerLevel level, int circle) {
        return circle(level) >= circle;
    }

    public static boolean hasReached(Player player, int circle) {
        return circle(player) >= circle;
    }

    /** Raises the world's Circle; a value that is not higher than the current one does nothing. Server side only. */
    public static boolean setCircle(MinecraftServer server, int circle) {
        return circle > circle(server) && apply(server, circle);
    }

    public static boolean setCircle(Player player, int circle) {
        return player.level().getServer() != null && setCircle(player.level().getServer(), circle);
    }

    /** Debug only: sets the world's Circle to any valid value, lowering it too. */
    public static boolean forceCircle(MinecraftServer server, int circle) {
        return clamp(circle) != circle(server) && apply(server, clamp(circle));
    }

    private static boolean apply(MinecraftServer server, int circle) {
        WorldCircle data = WorldCircle.get(server);
        int old = data.circle();
        data.set(circle);
        PacketDistributor.sendToAllPlayers(new CirclePayload(circle));
        NeoForge.EVENT_BUS.post(new CircleChangedEvent(server, old, circle));
        return true;
    }

    /** Sent to a player who just logged in, so a late joiner sees the right state. */
    public static void syncTo(net.minecraft.server.level.ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CirclePayload(circle(player.server)));
    }

    public static void setClientCircle(int circle) {
        clientCircle = clamp(circle);
    }
}
