package dev.zsskayr.merlins_inferno.event;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import dev.zsskayr.merlins_inferno.entity.OstaraEntity;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.worldgen.ModStructures;

/**
 * Ostara's only natural appearance: at dawn of every third world day she is called to a Rowanwood tree that is loaded near
 * a player, and she leaves at dusk (see {@link OstaraEntity#tick}). Only chunks that are already loaded are considered,
 * so she never forces the world to generate, and there is at most one Ostara per tree.
 */
public class OstaraSpawnHandler {
    private static final long DAY_LENGTH = 24000L;
    /** Ticks after sunrise in which the ritual may fire. */
    private static final long DAWN_WINDOW = 600L;
    private static final int DAY_INTERVAL = 3;
    private static final int SEARCH_CHUNKS = 10;
    private static final int TRIES = 16;

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % 20L != 0L || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        long time = level.getDayTime();
        long day = time / DAY_LENGTH;
        if (time % DAY_LENGTH >= DAWN_WINDOW || day % DAY_INTERVAL != 0 || level.getData(ModAttachments.OSTARA_LAST_DAY) == day
                || level.players().isEmpty()) {
            return;
        }
        Structure tree = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ModStructures.ROWANWOOD_TREE);
        if (tree == null) {
            return;
        }
        level.setData(ModAttachments.OSTARA_LAST_DAY, day);

        Set<ChunkPos> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos centre = player.chunkPosition();
            for (int dx = -SEARCH_CHUNKS; dx <= SEARCH_CHUNKS; dx++) {
                for (int dz = -SEARCH_CHUNKS; dz <= SEARCH_CHUNKS; dz++) {
                    ChunkPos chunk = new ChunkPos(centre.x + dx, centre.z + dz);
                    if (!level.hasChunk(chunk.x, chunk.z)) {
                        continue;
                    }
                    for (StructureStart start : level.structureManager().startsForStructure(chunk, s -> s == tree)) {
                        if (start.isValid() && seen.add(start.getChunkPos())) {
                            this.callOstara(level, start, day);
                        }
                    }
                }
            }
        }
    }

    private void callOstara(ServerLevel level, StructureStart start, long day) {
        BlockPos centre = start.getBoundingBox().getCenter();
        if (!level.getEntitiesOfClass(OstaraEntity.class, new AABB(centre).inflate(48.0)).isEmpty()) {
            return;
        }
        for (int i = 0; i < TRIES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 6.0 + level.random.nextDouble() * 8.0;
            int x = centre.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = centre.getZ() + (int) Math.round(Math.sin(angle) * radius);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                continue;
            }
            OstaraEntity ostara = ModEntityTypes.OSTARA.get().create(level);
            if (ostara == null) {
                return;
            }
            ostara.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            ostara.setDawnDay(day);
            ostara.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            level.addFreshEntity(ostara);
            level.sendParticles(ParticleTypes.CHERRY_LEAVES, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 0.6, 1.0, 0.6, 0.05);
            return;
        }
    }
}
