package dev.zsskayr.merlins_inferno.event;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.CircleChangedEvent;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.feature.BlockPaletteSwapProcessor;

/**
 * Red Lyrium spikes break out of the Oblivion once the world reaches Circle 3, in chunks that already exist as well as
 * new ones, the same way {@link ArganiteRetrogen} works: loaded chunks are queued and rolled a few per tick, once each.
 * A chunk players have already lived in is left alone (marked, never built on), and a spike is only pasted when every
 * chunk under its footprint is loaded, so nothing is generated or cut off at an unloaded border.
 */
public class RedLyriumSpikeRetrogen {
    private static final int CHUNKS_PER_TICK = 2;
    /** Ticks players may have spent in a chunk before it counts as theirs: about a minute. */
    private static final long UNTOUCHED_TICKS = 1200;
    private static final int THORN_CHANCE = 40, CRYSTAL_CHANCE = 70, GIANT_CHANCE = 220;
    private static final int MAX_UNEVENNESS = 4;
    private static final long SALT = 0x5245444C_59524955L;

    private static final ResourceLocation[] THORNS = templates("red_spike/red_thorn_", 1, 14);
    private static final ResourceLocation[] CRYSTALS = concat(templates("frozen_spike/lyrium_west_", 1, 12),
            templates("frozen_spike/lyrium_east_", 2, 8));
    private static final ResourceLocation GIANT = id("frozen_spike/lyrium_east_01");

    private final Queue<ChunkPos> queue = new ConcurrentLinkedQueue<>();
    private final Set<ChunkPos> queued = ConcurrentHashMap.newKeySet();
    private volatile boolean active;

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        queue.clear();
        queued.clear();
        active = ProgressionHelper.circle(event.getServer()) >= ProgressionHelper.THIRD_CIRCLE;
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        active = false;
        queue.clear();
        queued.clear();
    }

    @SubscribeEvent
    public void onCircleChanged(CircleChangedEvent event) {
        active = event.getNewCircle() >= ProgressionHelper.THIRD_CIRCLE;
        ServerLevel oblivion = event.getServer().getLevel(OblivionDimension.LEVEL);
        if (active && event.isIncrease() && oblivion != null) enqueueLoadedChunks(oblivion);
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (active && event.getLevel() instanceof ServerLevel level && level.dimension() == OblivionDimension.LEVEL
                && event.getChunk() instanceof LevelChunk chunk && !chunk.getData(ModAttachments.RED_SPIKES_DONE)) {
            enqueue(chunk.getPos());
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!active || queue.isEmpty()) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(OblivionDimension.LEVEL);
        if (level == null || ProgressionHelper.circle(server) < ProgressionHelper.THIRD_CIRCLE) return;
        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            ChunkPos pos = queue.poll();
            if (pos == null) return;
            queued.remove(pos);
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null || chunk.getData(ModAttachments.RED_SPIKES_DONE)) continue;
            if (chunk.getInhabitedTime() <= UNTOUCHED_TICKS) roll(level, chunk);
            chunk.setData(ModAttachments.RED_SPIKES_DONE, true);
            chunk.setUnsaved(true);
        }
    }

    private void enqueue(ChunkPos pos) {
        if (queued.add(pos)) queue.add(pos);
    }

    private void enqueueLoadedChunks(ServerLevel level) {
        int radius = level.getServer().getPlayerList().getViewDistance() + 1;
        Set<ChunkPos> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos centre = player.chunkPosition();
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(centre.x + dx, centre.z + dz);
                if (!seen.add(pos)) continue;
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk != null && !chunk.getData(ModAttachments.RED_SPIKES_DONE)) enqueue(pos);
            }
        }
    }

    private static void roll(ServerLevel level, LevelChunk chunk) {
        ChunkPos cp = chunk.getPos();
        RandomSource random = RandomSource.create(level.getSeed() ^ SALT ^ (cp.x * 341873128712L) ^ (cp.z * 132897987541L));
        if (random.nextInt(GIANT_CHANCE) == 0) {
            place(level, cp, random, GIANT, true);
        } else if (random.nextInt(CRYSTAL_CHANCE) == 0) {
            place(level, cp, random, CRYSTALS[random.nextInt(CRYSTALS.length)], true);
        } else if (random.nextInt(THORN_CHANCE) == 0) {
            place(level, cp, random, THORNS[random.nextInt(THORNS.length)], false);
        }
    }

    private static void place(ServerLevel level, ChunkPos cp, RandomSource random, ResourceLocation id, boolean recolour) {
        StructureTemplate template = level.getStructureManager().getOrCreate(id);
        Vec3i size = template.getSize(Rotation.NONE);
        if (size.getX() == 0) return;
        int cx = cp.getMinBlockX() + random.nextInt(16), cz = cp.getMinBlockZ() + random.nextInt(16);
        BlockPos corner = new BlockPos(cx - size.getX() / 2, 0, cz - size.getZ() / 2);
        // Every chunk under the footprint must already be loaded: nothing is generated, nothing is cut off.
        for (int x = corner.getX() >> 4; x <= (corner.getX() + size.getX() - 1) >> 4; x++)
            for (int z = corner.getZ() >> 4; z <= (corner.getZ() + size.getZ() - 1) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return;
        // Rooted on solid ground, under lava too: lakes get spikes rising out of them, never one standing on air.
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int x = 0; x <= size.getX(); x += 2) for (int z = 0; z <= size.getZ(); z += 2) {
            int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR, corner.getX() + x, corner.getZ() + z);
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        if (max - min > MAX_UNEVENNESS || min <= level.getMinBuildHeight() + 1 || min + size.getY() >= level.getMaxBuildHeight()) return;
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE)
                .setIgnoreEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        if (recolour) {
            settings.addProcessor(new BlockPaletteSwapProcessor(Map.of(
                    ModBlocks.LYRIUM_BLOCK.get(), ModBlocks.RED_LYRIUM_BLOCK.get(),
                    ModBlocks.LYRIUM_CLUSTER.get(), ModBlocks.RED_LYRIUM_CLUSTER.get(),
                    Blocks.CYAN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE)));
        }
        BlockPos at = corner.atY(min - 1);
        if (template.placeInWorld(level, at, at, settings, random, Block.UPDATE_CLIENTS)) {
            breakGround(level, at, size, random);
        }
    }

    /**
     * Broken ground around the spike's foot, so it reads as having torn out of the earth: the surface around the base
     * cracks into Blightstone and Corrupted Obsidian, with loose chunks and the odd Red Lyrium shard lying on top.
     * The ring grows with the base, and only touches chunks that are already loaded.
     */
    private static void breakGround(ServerLevel level, BlockPos at, Vec3i size, RandomSource random) {
        Set<Long> base = new HashSet<>();
        for (int x = 0; x < size.getX(); x++) for (int z = 0; z < size.getZ(); z++) for (int y = 0; y <= 2; y++) {
            BlockPos pos = at.offset(x, y, z);
            if (isBase(level.getBlockState(pos).getBlock(), y)) base.add(BlockPos.asLong(pos.getX(), 0, pos.getZ()));
        }
        if (base.isEmpty()) return;
        int radius = Math.min(5, 2 + base.size() / 12);
        BlockState[] floor = {ModBlocks.BLIGHTSTONE.get().defaultBlockState(), ModBlocks.CORRUPTED_OBSIDIAN.get().defaultBlockState(),
                ModBlocks.BLIGHTSTONE.get().defaultBlockState()};
        Set<Long> done = new HashSet<>();
        for (long packed : base) {
            int bx = BlockPos.getX(packed), bz = BlockPos.getZ(packed);
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                int x = bx + dx, z = bz + dz;
                long key = BlockPos.asLong(x, 0, z);
                if (base.contains(key) || !done.add(key)) continue;
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                double distance = nearest(base, x, z);
                if (distance > radius || random.nextDouble() > 1.15 - distance / radius) continue;
                BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1, z);
                if (level.getBlockState(ground).isAir() || isSpike(level.getBlockState(ground).getBlock())) continue;
                level.setBlock(ground, floor[random.nextInt(floor.length)], Block.UPDATE_CLIENTS);
                BlockPos above = ground.above();
                if (!level.getBlockState(above).isAir()) continue;
                float roll = random.nextFloat();
                if (distance <= 2 && roll < 0.22F) {
                    level.setBlock(above, floor[random.nextInt(floor.length)], Block.UPDATE_CLIENTS);
                } else if (roll < 0.06F) {
                    level.setBlock(above, ModBlocks.RED_LYRIUM_CLUSTER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** The thorns stand on their own Blightstone and Corrupted Obsidian, so those count as base right at the foot. */
    private static boolean isBase(Block block, int y) {
        return isSpike(block) || (y >= 1 && (block == ModBlocks.CORRUPTED_OBSIDIAN.get() || block == ModBlocks.BLIGHTSTONE.get()));
    }

    private static boolean isSpike(Block block) {
        return block == ModBlocks.RED_LYRIUM_BLOCK.get() || block == ModBlocks.RED_LYRIUM_CLUSTER.get()
                || block == Blocks.RED_STAINED_GLASS_PANE;
    }

    private static double nearest(Set<Long> base, int x, int z) {
        double best = Double.MAX_VALUE;
        for (long packed : base) {
            double dx = BlockPos.getX(packed) - x, dz = BlockPos.getZ(packed) - z;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return Math.sqrt(best);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, path);
    }

    private static ResourceLocation[] templates(String prefix, int from, int to) {
        ResourceLocation[] out = new ResourceLocation[to - from + 1];
        for (int i = from; i <= to; i++) out[i - from] = id(prefix + (i < 10 ? "0" + i : Integer.toString(i)));
        return out;
    }

    private static ResourceLocation[] concat(ResourceLocation[] a, ResourceLocation[] b) {
        ResourceLocation[] out = new ResourceLocation[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
