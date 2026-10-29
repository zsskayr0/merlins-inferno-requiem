package dev.zsskayr.merlins_inferno.event;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import dev.zsskayr.merlins_inferno.attachment.CircleChangedEvent;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Veins of Arganite appear in the Overworld once the world reaches Circle 2, in chunks that already exist as well
 * as new ones. A loaded chunk without the {@link ModAttachments#ARGANITE_VEINS_DONE} mark is queued (never generated
 * inside the load event) and a couple of chunks are done per server tick, each only inside its own bounds so no
 * neighbour is ever loaded. Chunks that unload before their turn are simply handled when they load again.
 */
public class ArganiteRetrogen {
    private static final int CHUNKS_PER_TICK = 2;
    private static final int MIN_ATTEMPTS = 2, MAX_ATTEMPTS = 4;
    private static final float ATTEMPT_CHANCE = 0.5F;
    private static final int MIN_VEIN_SIZE = 6, MAX_VEIN_SIZE = 9;
    private static final int MIN_Y = -64, MAX_Y = 64;
    private static final long SALT = 0x41524741_4E495445L;

    private final Queue<ChunkPos> queue = new ConcurrentLinkedQueue<>();
    private final Set<ChunkPos> queued = ConcurrentHashMap.newKeySet();
    /** Mirrors "the world is in Circle 2 or later" so the (possibly off-thread) load event never touches world data. */
    private volatile boolean active;

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        queue.clear();
        queued.clear();
        active = ProgressionHelper.circle(event.getServer()) >= ProgressionHelper.SECOND_CIRCLE;
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        active = false;
        queue.clear();
        queued.clear();
    }

    @SubscribeEvent
    public void onCircleChanged(CircleChangedEvent event) {
        active = event.getNewCircle() >= ProgressionHelper.SECOND_CIRCLE;
        if (active && event.isIncrease()) {
            enqueueLoadedChunks(event.getServer().overworld());
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (active && event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                && event.getChunk() instanceof LevelChunk chunk && !chunk.getData(ModAttachments.ARGANITE_VEINS_DONE)) {
            enqueue(chunk.getPos());
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!active || queue.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (ProgressionHelper.circle(server) < ProgressionHelper.SECOND_CIRCLE) {
            return;
        }
        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            ChunkPos pos = queue.poll();
            if (pos == null) {
                return;
            }
            queued.remove(pos);
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null && !chunk.getData(ModAttachments.ARGANITE_VEINS_DONE)) {
                placeVeins(level, chunk);
                chunk.setData(ModAttachments.ARGANITE_VEINS_DONE, true);
                chunk.setUnsaved(true);
            }
        }
    }

    private void enqueue(ChunkPos pos) {
        if (queued.add(pos)) {
            queue.add(pos);
        }
    }

    /** Every chunk currently loaded around a player; ones loaded elsewhere (forced chunks) are caught when they reload. */
    private void enqueueLoadedChunks(ServerLevel level) {
        int radius = level.getServer().getPlayerList().getViewDistance() + 1;
        Set<ChunkPos> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos centre = player.chunkPosition();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    ChunkPos pos = new ChunkPos(centre.x + dx, centre.z + dz);
                    if (seen.add(pos)) {
                        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                        if (chunk != null && !chunk.getData(ModAttachments.ARGANITE_VEINS_DONE)) {
                            enqueue(pos);
                        }
                    }
                }
            }
        }
    }

    private static void placeVeins(ServerLevel level, LevelChunk chunk) {
        ChunkPos cp = chunk.getPos();
        RandomSource random = RandomSource.create(level.getSeed() ^ SALT ^ (cp.x * 341873128712L) ^ (cp.z * 132897987541L));
        BlockState arganite = ModBlocks.ARGANITE.get().defaultBlockState();
        int attempts = MIN_ATTEMPTS + random.nextInt(MAX_ATTEMPTS - MIN_ATTEMPTS + 1);
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (random.nextFloat() >= ATTEMPT_CHANCE) {
                continue;
            }
            int size = MIN_VEIN_SIZE + random.nextInt(MAX_VEIN_SIZE - MIN_VEIN_SIZE + 1);
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y + 1);
            int placed = 0;
            // a random walk clipped to the chunk's own columns and the height band, until the vein has its blocks
            for (int step = 0; step < size * 4 && placed < size; step++) {
                BlockPos pos = new BlockPos(cp.getMinBlockX() + x, y, cp.getMinBlockZ() + z);
                BlockState state = chunk.getBlockState(pos);
                if (state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                    level.setBlock(pos, arganite, Block.UPDATE_CLIENTS);
                    placed++;
                }
                x = Math.max(0, Math.min(15, x + random.nextInt(3) - 1));
                z = Math.max(0, Math.min(15, z + random.nextInt(3) - 1));
                y = Math.max(MIN_Y, Math.min(MAX_Y, y + random.nextInt(3) - 1));
            }
        }
    }
}
