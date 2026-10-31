package dev.zsskayr.merlins_inferno.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.meteor.MeteorData;
import dev.zsskayr.merlins_inferno.meteor.MeteorData.Meteor;
import dev.zsskayr.merlins_inferno.meteor.MeteorSite;

/**
 * Fallen stars, the Celestial route's source of Luminite. Once the world has reached Circle 2, each night has a chance to drop a
 * meteor somewhere within {@value #MAX_DISTANCE} blocks of a player; the chat names an approximate spot. A meteor is only a
 * coordinate until its chunks load (nobody is ever 3000 blocks from a loaded chunk), then the crater is built; once a player
 * reaches it, or {@value #LIFETIME_DAYS} days pass, it is forgotten. The world holds at most {@value #MAX_ACTIVE} at a time.
 */
public class MeteorHandler {
    public static final int MAX_DISTANCE = 3000;
    public static final int MIN_DISTANCE = 500;
    public static final int MAX_ACTIVE = 3;
    public static final int LIFETIME_DAYS = 7;
    public static final double NIGHTLY_CHANCE = 0.3;
    /** How far the announced spot is from the real one, at most. */
    public static final int ANNOUNCE_ERROR = 300;
    /** A meteor is claimed when a player stands this close to its centre. */
    public static final int CLAIM_DISTANCE = 24;
    private static final int NIGHT_START = 13000;
    private static final int CHECK_INTERVAL = 40;

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL != 0 || !ProgressionHelper.hasReached(level, ProgressionHelper.SECOND_CIRCLE)) {
            return;
        }
        MeteorData data = MeteorData.get(level);
        long day = level.getDayTime() / 24000L;
        if (level.getDayTime() % 24000L >= NIGHT_START && data.lastNight() != day) {
            data.rolledNight(day);
            if (level.random.nextDouble() < NIGHTLY_CHANCE) {
                fall(level, data, level.random);
            }
        }
        tend(level, data);
    }

    /** {@code /merlins_inferno debug meteor}: forces a meteor to fall now, ignoring the chance, the night and the active cap. */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("merlins_inferno")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("meteor").executes(context -> {
                            ServerLevel level = context.getSource().getServer().overworld();
                            Meteor meteor = fall(level, MeteorData.get(level), level.random, true);
                            if (meteor == null) {
                                context.getSource().sendFailure(Component.literal("No meteor fell: no player in the Overworld, or no spot inside the world border."));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Meteor fell at " + meteor.x + ", " + meteor.z + " (built when its chunks load)."), true);
                            return 1;
                        }))));
    }

    /** Drops a meteor near a random player, if the world has room for one. */
    public static Meteor fall(ServerLevel level, MeteorData data, RandomSource random) {
        return fall(level, data, random, false);
    }

    public static Meteor fall(ServerLevel level, MeteorData data, RandomSource random, boolean force) {
        List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty() || (!force && data.active() >= MAX_ACTIVE)) {
            return null;
        }
        ServerPlayer near = players.get(random.nextInt(players.size()));
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            int x = near.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = near.getBlockZ() + (int) (Math.sin(angle) * distance);
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 64, z))) {
                continue;
            }
            Meteor meteor = data.add(x, z, level.getGameTime(), level.getSeaLevel());
            announce(level, meteor, random);
            return meteor;
        }
        return null;
    }

    /** The chat line: an approximate position, rounded to 100 blocks around a random offset from the real one. */
    private static void announce(ServerLevel level, Meteor meteor, RandomSource random) {
        int x = roundTo100(meteor.x + random.nextInt(ANNOUNCE_ERROR * 2 + 1) - ANNOUNCE_ERROR);
        int z = roundTo100(meteor.z + random.nextInt(ANNOUNCE_ERROR * 2 + 1) - ANNOUNCE_ERROR);
        Component message = Component.translatable("message.merlins_inferno.meteor_fell", x, z)
                .withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.ITALIC);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.8F, 0.6F);
        }
    }

    private static int roundTo100(int value) {
        return Math.round(value / 100.0F) * 100;
    }

    /** Builds the craters whose chunks have loaded, and forgets the claimed and the stale. */
    public static void tend(ServerLevel level, MeteorData data) {
        List<Meteor> gone = new ArrayList<>();
        for (Meteor meteor : data.all()) {
            if (level.getGameTime() - meteor.fell > LIFETIME_DAYS * 24000L) {
                gone.add(meteor);
            } else if (!meteor.built) {
                if (MeteorSite.canBuild(level, meteor.x, meteor.z)) {
                    meteor.y = MeteorSite.build(level, meteor.x, meteor.z, level.random);
                    meteor.built = true;
                    data.touch();
                }
            } else if (claimed(level, meteor)) {
                gone.add(meteor);
            }
        }
        gone.forEach(data::remove);
    }

    private static boolean claimed(ServerLevel level, Meteor meteor) {
        return level.getNearestPlayer(meteor.x + 0.5, meteor.y, meteor.z + 0.5, CLAIM_DISTANCE, p -> !p.isSpectator()) != null;
    }
}
