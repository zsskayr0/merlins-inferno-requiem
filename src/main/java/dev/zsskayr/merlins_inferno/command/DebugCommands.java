package dev.zsskayr.merlins_inferno.command;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/**
 * Debug commands (op level 2). {@code /mi_debug circle <C1|C2|C3>} sets the world's Circle, up or down (the only
 * way down); without an argument it shows the current one. {@code /mi_circle} shows the same to anyone, no op needed.
 */
public class DebugCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mi_circle").executes(context -> showCircle(context.getSource())));
        event.getDispatcher().register(Commands.literal("mi_debug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("cinder").executes(context -> cinderStatus(context.getSource().getPlayerOrException())))
                .then(Commands.literal("circle")
                        .executes(context -> showCircle(context.getSource()))
                        .then(Commands.argument("circle", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[] {"C1", "C2", "C3"}, builder))
                                .executes(context -> setCircle(context.getSource(), StringArgumentType.getString(context, "circle"))))));
    }

    private static int showCircle(CommandSourceStack source) {
        int circle = ProgressionHelper.circle(source.getServer());
        source.sendSuccess(() -> Component.literal("[Merlin's Inferno] World Circle: C" + circle), false);
        return circle;
    }

    private static int setCircle(CommandSourceStack source, String text) {
        int circle = switch (text.toUpperCase(java.util.Locale.ROOT)) {
            case "C1" -> ProgressionHelper.FIRST_CIRCLE;
            case "C2" -> ProgressionHelper.SECOND_CIRCLE;
            case "C3" -> ProgressionHelper.THIRD_CIRCLE;
            default -> 0;
        };
        if (circle == 0) {
            source.sendFailure(Component.literal("[Merlin's Inferno] Unknown Circle '" + text + "' - use C1, C2 or C3"));
            return 0;
        }
        boolean changed = ProgressionHelper.forceCircle(source.getServer(), circle);
        source.sendSuccess(() -> Component.literal("[Merlin's Inferno] World Circle " + (changed ? "set to C" : "already C") + circle), true);
        return circle;
    }

    private static int cinderStatus(ServerPlayer player) {
        boolean eligible = player.serverLevel().dimension() == dev.zsskayr.merlins_inferno.portal.OblivionDimension.LEVEL
                && ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE) && !player.isSpectator()
                && player.serverLevel().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                && player.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING);
        var nearby = player.serverLevel().getEntitiesOfClass(dev.zsskayr.merlins_inferno.entity.CinderEntity.class,
                player.getBoundingBox().inflate(160), entity -> entity.isAlive());
        player.sendSystemMessage(Component.literal("[Cinder] Natural spawn eligible: " + eligible
                + "; world Circle: C" + ProgressionHelper.circle(player) + "; loaded within 160 blocks: " + nearby.size()
                + "; spawn cooldown: " + Math.max(0, (player.getPersistentData().getLong("MerlinsCinderNextSpawn") - player.serverLevel().getGameTime()) / 20) + "s"));
        nearby.stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).ifPresent(c ->
                player.sendSystemMessage(Component.literal("[Cinder] Nearest: " + c.blockPosition().toShortString()
                        + "; action: " + c.action().clip + "; cold: " + c.coldTicks() + "/600; fully submerged: " + c.fullySubmerged())));
        return nearby.size();
    }
}
