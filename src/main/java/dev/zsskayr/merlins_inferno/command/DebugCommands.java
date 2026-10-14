package dev.zsskayr.merlins_inferno.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/**
 * Debug commands (op level 2). {@code /mi_debug c2} jumps straight to Circle 2 without the Pandora Box ritual;
 * {@code /mi_debug circle <1|2>} sets the Circle explicitly (to go back to Circle 1 for testing).
 */
public class DebugCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mi_debug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("c2").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return setCircle(player, ProgressionHelper.SECOND_CIRCLE);
                }))
                .then(Commands.literal("circle")
                        .then(Commands.argument("circle", IntegerArgumentType.integer(
                                ProgressionHelper.FIRST_CIRCLE, ProgressionHelper.SECOND_CIRCLE))
                                .executes(context -> setCircle(context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "circle"))))));
    }

    private static int setCircle(ServerPlayer player, int circle) {
        ProgressionHelper.setCircle(player, circle);
        player.sendSystemMessage(Component.literal("[Merlin's Inferno] Circle set to " + circle));
        return circle;
    }
}
