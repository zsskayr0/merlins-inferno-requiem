package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrain;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrains;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * El'ysium's real generator (noise, biomes, surface rules) run on bare chunks off the server thread, as in
 * {@link BlightstoneGameTests}. Dumps a top-down picture to build/elysium-test/ and checks the world matches the pure
 * terrain function: floating islands of Arganite, grass and sand, with biomes that follow the climate.
 */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class ElysiumGameTests {
    private static final int RADIUS_CHUNKS = 20;

    private record Result(Map<String, Integer> biomes, Map<String, Integer> blocks, int mismatches, int sampled, String center) {}

    @GameTest(template = "bench_empty", timeoutTicks = 24000)
    public static void elysiumGeneratesFloatingIslands(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        net.minecraft.world.level.dimension.LevelStem stem;
        try (var reader = level.getServer().getResourceManager()
                .openAsReader(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "dimension/elysium.json"))) {
            var json = com.google.gson.JsonParser.parseReader(reader);
            stem = net.minecraft.world.level.dimension.LevelStem.CODEC
                    .parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, access), json)
                    .getOrThrow(IllegalStateException::new);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.assertTrue(stem.generator() instanceof NoiseBasedChunkGenerator, "El'ysium has a noise generator");
        var gen = (NoiseBasedChunkGenerator) stem.generator();
        var biomes = access.registryOrThrow(Registries.BIOME);
        RandomState randomState = RandomState.create(gen.generatorSettings().value(), access.lookupOrThrow(Registries.NOISE), 4242L);
        LevelHeightAccessor height = LevelHeightAccessor.create(0, 384);

        AtomicReference<Result> done = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                ElysiumTerrain terrain = ElysiumTerrains.forNoise(randomState.getOrCreateNoise(ElysiumTerrains.NOISE_KEY));
                ElysiumTerrain.Column col = new ElysiumTerrain.Column();
                // a point well inside some island, found by walking outward from the origin
                int cx0 = 0, cz0 = 0;
                search:
                for (int r = 0; r < 12000; r += 64) {
                    for (int i = 0; i < Math.max(1, r / 32); i++) {
                        double a = i * 2 * Math.PI / Math.max(1, r / 32);
                        int x = (int) (r * Math.cos(a)), z = (int) (r * Math.sin(a));
                        terrain.sample(x, z, col);
                        if (col.land && col.inland > 260) {
                            cx0 = x >> 4;
                            cz0 = z >> 4;
                            break search;
                        }
                    }
                }
                int n = RADIUS_CHUNKS * 2;
                BufferedImage img = new BufferedImage(n * 16, n * 16, BufferedImage.TYPE_INT_RGB);
                Map<String, Integer> biomeTally = new TreeMap<>(), blockTally = new TreeMap<>();
                int mismatches = 0, sampled = 0;
                for (int dx = -RADIUS_CHUNKS; dx < RADIUS_CHUNKS; dx++) {
                    for (int dz = -RADIUS_CHUNKS; dz < RADIUS_CHUNKS; dz++) {
                        int cx = cx0 + dx, cz = cz0 + dz;
                        ChunkAccess chunk = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.EMPTY, height, biomes, null);
                        gen.createBiomes(randomState, Blender.empty(), level.structureManager(), chunk).join();
                        ((ProtoChunk) chunk).setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
                        gen.fillFromNoise(Blender.empty(), randomState, level.structureManager(), chunk).join();
                        BiomeManager manager = new BiomeManager((x, y, z) -> chunk.getNoiseBiome(x, y, z), 4242L);
                        gen.buildSurface(chunk, new WorldGenerationContext(gen, height), randomState, level.structureManager(),
                                manager, biomes, Blender.empty());
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                int wx = (cx << 4) + x, wz = (cz << 4) + z;
                                int top = highest(chunk, x, z) + 1;
                                int px = (dx + RADIUS_CHUNKS) * 16 + x, pz = (dz + RADIUS_CHUNKS) * 16 + z;
                                if (top <= 0 && chunk.getBlockState(new BlockPos(wx, 0, wz)).isAir()) {
                                    img.setRGB(px, pz, 0x2b3138);
                                    continue;
                                }
                                BlockPos pos = new BlockPos(wx, top - 1, wz);
                                BlockState state = chunk.getBlockState(pos);
                                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                                blockTally.merge(id, 1, Integer::sum);
                                if ((x & 3) == 0 && (z & 3) == 0) {
                                    String biome = manager.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("?");
                                    biomeTally.merge(biome, 1, Integer::sum);
                                }
                                terrain.sample(wx, wz, col);
                                sampled++;
                                if (!col.land || Math.abs(top - 1 - col.top) > 3) {
                                    mismatches++;
                                }
                                img.setRGB(px, pz, color(state, top));
                            }
                        }
                    }
                }
                for (var sp : ElysiumTerrains.spans(terrain).near(-2500, -2500, 2500, 2500)) {
                    if (sp.hasBridge() && sp.length() > 100) {
                        System.out.println("[ElysiumSpan] A=" + (int) sp.ax() + "," + (int) sp.ay() + "," + (int) sp.az() + " B=" + (int) sp.bx() + "," + (int) sp.by() + "," + (int) sp.bz() + " len=" + (int) sp.length());
                    }
                }
                File out = new File("build/elysium-test");
                out.mkdirs();
                javax.imageio.ImageIO.write(img, "png", new File(out, "top.png"));
                done.set(new Result(biomeTally, blockTally, mismatches, sampled, (cx0 << 4) + "," + (cz0 << 4)));
            } catch (Throwable t) {
                t.printStackTrace();
                failure.set(t);
            }
        }, "elysium-chunkgen");
        worker.setDaemon(true);
        worker.start();

        helper.succeedWhen(() -> {
            if (done.get() == null && failure.get() == null) {
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            helper.assertTrue(failure.get() == null, "Chunk generation failed: " + failure.get());
            Result result = done.get();
            helper.assertTrue(result != null, "Still generating El'ysium chunks");
            System.out.println("[ElysiumTest] around " + result.center() + " blocks " + result.blocks() + " biomes " + result.biomes()
                    + " mismatches " + result.mismatches() + "/" + result.sampled());
            helper.assertTrue(result.blocks().getOrDefault(ModBlocks.ARGANITE.getId().toString(), 0) + result.blocks().getOrDefault("minecraft:grass_block", 0) > 0,
                    "El'ysium generates stone and grass: " + result.blocks());
            helper.assertTrue(result.mismatches() * 20 <= result.sampled(), "Generated surface follows ElysiumTerrain: " + result.mismatches() + "/" + result.sampled());
        });
    }

    /** The y of the highest non-air block (the bare chunk's heightmaps are not refreshed by the surface rules). */
    private static int highest(ChunkAccess chunk, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 383; y >= 0; y--) {
            if (!chunk.getBlockState(pos.set(x, y, z)).isAir()) {
                return y;
            }
        }
        return -1;
    }

    private static int color(BlockState state, int top) {
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        int base = switch (id) {
            case "grass_block" -> 0x7fa07a;
            case "sand" -> 0xdad2b4;
            case "snow_block" -> 0xf2f4f6;
            case "gravel" -> 0x8d8f91;
            case "mud" -> 0x5b5149;
            case "clay" -> 0x9aa3ad;
            case "dirt" -> 0x7a6a58;
            case "arganite" -> 0xd0d4d8;
            case "water" -> 0x4f7c93;
            default -> 0xff00ff;
        };
        double shade = 0.7 + 0.3 * (top - 150) / 150.0;
        shade = Math.max(0.5, Math.min(1.3, shade));
        int r = (int) Math.min(255, ((base >> 16) & 255) * shade), g = (int) Math.min(255, ((base >> 8) & 255) * shade), b = (int) Math.min(255, (base & 255) * shade);
        return (r << 16) | (g << 8) | b;
    }
}
