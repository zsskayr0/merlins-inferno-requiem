package dev.zsskayr.merlins_inferno.attachment;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import dev.zsskayr.merlins_inferno.network.CirclePayload;

/**
 * The world's Circle: 1 until the Pandora Box ritual is performed by anyone, 2 after it; 3 is a valid value that does
 * nothing yet. The ritual only raises it; {@code /gamerule circle <1-3>} sets it either way. The value is the world's
 * {@code circle} game rule, so it is saved with the world; clients keep a synced copy that {@link #circle(Player)}
 * reads on the logical client.
 */
public final class ProgressionHelper {
    public static final int FIRST_CIRCLE = 1;
    public static final int SECOND_CIRCLE = 2;
    public static final int THIRD_CIRCLE = 3;

    public static final GameRules.Key<GameRules.IntegerValue> CIRCLE_RULE = GameRules.register("circle",
            GameRules.Category.MISC, GameRules.IntegerValue.create(FIRST_CIRCLE, ProgressionHelper::onRuleChanged));

    /** What the rule held when it last changed (or the server started), to tell the event what it changed from. */
    private static volatile int known = FIRST_CIRCLE;

    /** The client's copy of the world's Circle, set by {@link CirclePayload}. */
    private static volatile int clientCircle = FIRST_CIRCLE;

    private ProgressionHelper() {
    }

    /** Loads the class so the game rule is registered before any world loads. */
    public static void init() {
    }

    static int clamp(int circle) {
        return Math.max(FIRST_CIRCLE, Math.min(THIRD_CIRCLE, circle));
    }

    public static int circle(MinecraftServer server) {
        return clamp(server.getGameRules().getInt(CIRCLE_RULE));
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
        if (circle <= circle(server)) {
            return false;
        }
        server.getGameRules().getRule(CIRCLE_RULE).set(clamp(circle), server);
        return true;
    }

    public static boolean setCircle(Player player, int circle) {
        return player.level().getServer() != null && setCircle(player.level().getServer(), circle);
    }

    /** Called once the server has loaded the rule from the save. */
    public static void onServerStarted(MinecraftServer server) {
        known = circle(server);
    }

    /** The rule's change callback: keeps it in 1-3, then tells the clients and the event bus. */
    private static void onRuleChanged(MinecraftServer server, GameRules.IntegerValue rule) {
        int value = rule.get();
        if (value != clamp(value)) {
            rule.set(clamp(value), server);
            return;
        }
        int old = known;
        known = value;
        if (old != value) {
            PacketDistributor.sendToAllPlayers(new CirclePayload(value));
            NeoForge.EVENT_BUS.post(new CircleChangedEvent(server, old, value));
        }
    }

    /** Sent to a player who just logged in, so a late joiner sees the right state. */
    public static void syncTo(net.minecraft.server.level.ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CirclePayload(circle(player.server)));
    }

    public static void setClientCircle(int circle) {
        clientCircle = clamp(circle);
    }
}
