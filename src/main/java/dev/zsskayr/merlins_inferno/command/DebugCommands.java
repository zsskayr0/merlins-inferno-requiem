package dev.zsskayr.merlins_inferno.command;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/** Debug commands (op level 2). The world's Circle is the {@code circle} game rule: {@code /gamerule circle <1-3>}. */
public class DebugCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mi_debug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("cinder").executes(context -> cinderStatus(context.getSource().getPlayerOrException()))));
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
