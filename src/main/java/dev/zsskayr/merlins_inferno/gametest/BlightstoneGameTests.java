package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Oblivion's stone: Blightstone everywhere, netherrack only under the Nether Wastes and the Carnal Heap. */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class BlightstoneGameTests {
    private record Tally(Map<String, int[]> perBiome) {}

    /**
     * The test server only loads the vanilla dimensions, so Oblivion's generator is taken straight from the level-stem
     * registry and run (noise, biomes, surface rules) on bare chunks, off the server thread. Counts stone per biome:
     * [0] blightstone, [1] netherrack.
     */
    @GameTest(template = "bench_empty", timeoutTicks = 12000)
    public static void oblivionStoneIsBlightstone(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        net.minecraft.world.level.dimension.LevelStem stem;
        try (var reader = level.getServer().getResourceManager()
                .openAsReader(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "dimension/oblivion.json"))) {
            var json = com.google.gson.JsonParser.parseReader(reader);
            stem = net.minecraft.world.level.dimension.LevelStem.CODEC
                    .parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, access), json)
                    .getOrThrow(IllegalStateException::new);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.assertTrue(stem.generator() instanceof NoiseBasedChunkGenerator, "Oblivion has a noise generator");
        var gen = (NoiseBasedChunkGenerator) stem.generator();
        var biomes = access.registryOrThrow(Registries.BIOME);
        RandomState randomState = RandomState.create(gen.generatorSettings().value(), access.lookupOrThrow(Registries.NOISE), 4242L);
        LevelHeightAccessor height = LevelHeightAccessor.create(0, 256);

        AtomicReference<Tally> done = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                Map<String, int[]> perBiome = new TreeMap<>();
                for (int cx = -32; cx <= 32; cx += 8) {
                    for (int cz = -32; cz <= 32; cz += 8) {
                        ChunkAccess chunk = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.EMPTY, height, biomes, null);
                        gen.createBiomes(randomState, Blender.empty(), level.structureManager(), chunk).join();
                        ((ProtoChunk) chunk).setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
                        gen.fillFromNoise(Blender.empty(), randomState, level.structureManager(), chunk).join();
                        BiomeManager manager = new BiomeManager((x, y, z) -> chunk.getNoiseBiome(x, y, z), 4242L);
                        gen.buildSurface(chunk, new WorldGenerationContext(gen, height), randomState, level.structureManager(),
                                manager, biomes, Blender.empty());
                        for (int x = 0; x < 16; x += 2) {
                            for (int z = 0; z < 16; z += 2) {
                                for (int y = 8; y < 200; y += 4) {
                                    BlockPos pos = new BlockPos((cx << 4) + x, y, (cz << 4) + z);
                                    var state = chunk.getBlockState(pos);
                                    boolean blight = state.is(ModBlocks.BLIGHTSTONE.get());
                                    boolean rack = state.is(Blocks.NETHERRACK);
                                    if (!blight && !rack) continue;
                                    String biome = manager.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?");
                                    perBiome.computeIfAbsent(biome, k -> new int[2])[blight ? 0 : 1]++;
                                }
                            }
                        }
                    }
                }
                done.set(new Tally(perBiome));
            } catch (Throwable t) {
                t.printStackTrace();
                failure.set(t);
            }
        }, "blightstone-chunkgen");
        worker.setDaemon(true);
        worker.start();

        helper.succeedWhen(() -> {
            // The test server ticks as fast as it can, so tick-based waiting would give up in seconds: pace it.
            if (done.get() == null && failure.get() == null) {
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            helper.assertTrue(failure.get() == null, "Chunk generation failed: " + failure.get());
            Tally tally = done.get();
            helper.assertTrue(tally != null, "Still generating Oblivion chunks");
            int blightTotal = 0;
            StringBuilder report = new StringBuilder("Oblivion stone per biome (blightstone/netherrack):");
            for (var e : tally.perBiome().entrySet()) {
                int blight = e.getValue()[0], rack = e.getValue()[1];
                blightTotal += blight;
                report.append("\n  ").append(e.getKey()).append(" ").append(blight).append("/").append(rack);
                boolean rackBiome = e.getKey().equals("minecraft:nether_wastes") || e.getKey().equals("merlins_inferno:carnal_heap");
                // A little netherrack can border a biome seam, so tolerate a sliver rather than demand zero.
                helper.assertTrue(rackBiome || rack * 50 <= blight + rack, "Netherrack leaked into " + e.getKey() + ": " + blight + "/" + rack);
            }
            helper.assertTrue(blightTotal > 0, "Blightstone generates in Oblivion");
        });
    }
}
