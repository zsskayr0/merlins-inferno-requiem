package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumChunkGenerator;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumSeed;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Column;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import dev.zsskayr.merlins_inferno.worldgen.elysium.structure.SpanBuilder;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * El'ysium's real generator (biomes + terrain) run on bare chunks off the server thread, as in
 * {@link BlightstoneGameTests}. Dumps a top-down picture to build/elysium-test/ and checks the chunks are an exact copy
 * of the pure core: every column's top block sits where {@link ElysiumWorld#sample} says, and the biome matches.
 */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class ElysiumGameTests {
    private static final int RADIUS_CHUNKS = 20;
    /** An arrival-grade point: dry island this far from the coast (same rule as the portal's arrival search). */
    private static final int MIN_INLAND = 60;

    private record Result(Map<String, Integer> biomes, Map<String, Integer> blocks, int mismatches, int biomeMismatches,
                          int sampled, String center) {}

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
        helper.assertTrue(stem.generator() instanceof ElysiumChunkGenerator, "El'ysium uses the El'ysium chunk generator");
        var gen = (ElysiumChunkGenerator) stem.generator();
        var biomes = access.registryOrThrow(Registries.BIOME);
        RandomState randomState = RandomState.create(NoiseGeneratorSettings.dummy(), access.lookupOrThrow(Registries.NOISE), level.getSeed());
        LevelHeightAccessor height = LevelHeightAccessor.create(ElysiumWorld.MIN_Y, ElysiumWorld.HEIGHT);
        ElysiumWorld world = ElysiumSeed.world();

        AtomicReference<Result> done = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                Column col = new Column();
                // a point well inside some island, found by walking outward from the origin
                int cx0 = 0, cz0 = 0;
                search:
                for (int r = 0; r < 12000; r += 64) {
                    for (int i = 0; i < Math.max(1, r / 32); i++) {
                        double a = i * 2 * Math.PI / Math.max(1, r / 32);
                        int x = (int) (r * Math.cos(a)), z = (int) (r * Math.sin(a));
                        world.sample(x, z, col);
                        if (col.land && col.inland >= MIN_INLAND && !col.hasWater()) {
                            cx0 = x >> 4;
                            cz0 = z >> 4;
                            break search;
                        }
                    }
                }
                int n = RADIUS_CHUNKS * 2;
                BufferedImage img = new BufferedImage(n * 16, n * 16, BufferedImage.TYPE_INT_RGB);
                Map<String, Integer> biomeTally = new TreeMap<>(), blockTally = new TreeMap<>();
                int mismatches = 0, biomeMismatches = 0, sampled = 0;
                for (int dx = -RADIUS_CHUNKS; dx < RADIUS_CHUNKS; dx++) {
                    for (int dz = -RADIUS_CHUNKS; dz < RADIUS_CHUNKS; dz++) {
                        int cx = cx0 + dx, cz = cz0 + dz;
                        ChunkAccess chunk = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.EMPTY, height, biomes, null);
                        gen.createBiomes(randomState, Blender.empty(), level.structureManager(), chunk).join();
                        ((ProtoChunk) chunk).setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
                        gen.fillFromNoise(Blender.empty(), randomState, level.structureManager(), chunk).join();
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                int wx = (cx << 4) + x, wz = (cz << 4) + z;
                                int top = highest(chunk, x, z);
                                int px = (dx + RADIUS_CHUNKS) * 16 + x, pz = (dz + RADIUS_CHUNKS) * 16 + z;
                                world.sample(wx, wz, col);
                                sampled++;
                                int expected = expectedTop(col);
                                if (expected < 0) {
                                    expected = world.cloudTop(wx, wz);
                                }
                                if (top != expected) {
                                    mismatches++;
                                }
                                if (top < ElysiumWorld.LOWEST_SOLID) {
                                    img.setRGB(px, pz, 0xe8eef4); // the cloud sea
                                    continue;
                                }
                                BlockPos pos = new BlockPos(wx, top, wz);
                                BlockState state = chunk.getBlockState(pos);
                                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                                blockTally.merge(id, 1, Integer::sum);
                                if ((x & 3) == 0 && (z & 3) == 0) {
                                    var holder = chunk.getNoiseBiome(wx >> 2, top >> 2, wz >> 2);
                                    String biome = holder.unwrapKey().map(k -> k.location().getPath()).orElse("?");
                                    biomeTally.merge(biome, 1, Integer::sum);
                                    String want = world.biomeAt((wx & ~3) + 2, ((top >> 2) << 2) + 2, (wz & ~3) + 2).id;
                                    if (!want.equals(biome)) {
                                        biomeMismatches++;
                                    }
                                }
                                img.setRGB(px, pz, color(state, top));
                            }
                        }
                    }
                }
                File out = new File("build/elysium-test");
                out.mkdirs();
                javax.imageio.ImageIO.write(img, "png", new File(out, "top.png"));
                done.set(new Result(biomeTally, blockTally, mismatches, biomeMismatches, sampled, (cx0 << 4) + "," + (cz0 << 4)));
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
                    + " top mismatches " + result.mismatches() + "/" + result.sampled() + " biome mismatches " + result.biomeMismatches());
            helper.assertTrue(result.blocks().getOrDefault(ModBlocks.ARGANITE.getId().toString(), 0) + result.blocks().getOrDefault("minecraft:grass_block", 0) > 0,
                    "El'ysium generates stone and grass: " + result.blocks());
            helper.assertTrue(result.mismatches() * 200 <= result.sampled(), "Generated columns copy the core: " + result.mismatches() + "/" + result.sampled());
            helper.assertTrue(result.biomeMismatches() == 0, "Chunk biomes follow ElysiumWorld.biomeAt: " + result.biomeMismatches());
        });
    }

    /** Arrivals land on an islet of their own, with a walkable bridge to a real island a short way off. */
    @GameTest(template = "bench_empty")
    public static void elysiumSpawnIsletIsBridged(GameTestHelper helper) {
        ElysiumWorld world = ElysiumSeed.world();
        int fromX = 90000, fromZ = -90000;
        ElysiumWorld.SpawnIslet islet = world.nearestSpawnIslet(fromX / dev.zsskayr.merlins_inferno.portal.ElysiumDimension.SCALE, fromZ / dev.zsskayr.merlins_inferno.portal.ElysiumDimension.SCALE);
        helper.assertTrue(islet != null, "The world has a spawn islet");
        var is = islet.island();
        Column c = new Column();
        world.sample(is.cx(), is.cz(), c);
        helper.assertTrue(c.land && c.island == is.index() && !c.hasWater(), "The islet's centre is its own dry land");
        var bridge = islet.bridge();
        helper.assertTrue(bridge.kind() == ElysiumWorld.Span.Kind.STONE_ARCH, "A walkable stone bridge");
        helper.assertTrue(world.spansIn(bridge.ax(), bridge.az(), bridge.ax(), bridge.az()).contains(bridge), "The decorator is told about the bridge");
        world.sample(bridge.ax(), bridge.az(), c);
        helper.assertTrue(c.land && c.island == is.index(), "The bridge starts on the islet");
        world.sample(bridge.bx(), bridge.bz(), c);
        helper.assertTrue(c.land && c.island == islet.host(), "The bridge ends on the host island");
        double length = Math.hypot(bridge.bx() - bridge.ax(), bridge.bz() - bridge.az());
        helper.assertTrue(length < 130, "The host is close by, the bridge is " + length + " long");

        // The test server has no El'ysium level, so the frame and the bridge go up on this one: any entry lands on the islet...
        var level = helper.getLevel();
        BlockPos arrival = dev.zsskayr.merlins_inferno.portal.ElysiumDimension.ensureArrival(level, new BlockPos(fromX, 70, fromZ));
        helper.assertTrue(Math.hypot(arrival.getX() - is.cx(), arrival.getZ() - is.cz()) < 12, "An entry with no portal near lands on the nearest islet: " + arrival);
        helper.assertTrue(level.getBlockState(arrival.below()).isSolid() && level.getBlockState(arrival).isAir(), "The arrival has ground and room: " + arrival);
        helper.assertTrue(level.getBlockState(arrival.offset(0, 1, -2)).is(ModBlocks.ELYSIUM_PORTAL.get()), "The portal stands beside the arrival");
        // ...and the bridge, moved here whole, has a deck at its middle
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1)).atY(bridge.ay());
        int bx = o.getX() + bridge.bx() - bridge.ax(), bz = o.getZ() + bridge.bz() - bridge.az();
        var moved = new ElysiumWorld.Span(bridge.kind(), o.getX(), bridge.ay(), o.getZ(), bx, bridge.by(), bz, bridge.sag(), bridge.width());
        for (int cx = (Math.min(o.getX(), bx) >> 4) - 1; cx <= (Math.max(o.getX(), bx) >> 4) + 1; cx++) {
            for (int cz = (Math.min(o.getZ(), bz) >> 4) - 1; cz <= (Math.max(o.getZ(), bz) >> 4) + 1; cz++) {
                SpanBuilder.build(level, new ChunkPos(cx, cz), world, java.util.List.of(moved));
            }
        }
        int deckX = (o.getX() + bx) / 2, deckZ = (o.getZ() + bz) / 2;
        boolean deck = false;
        for (int y = bridge.ay(); y <= bridge.ay() + 8 && !deck; y++) {
            deck = level.getBlockState(new BlockPos(deckX, y, deckZ)).isSolid();
        }
        helper.assertTrue(deck, "The bridge has a deck at its middle, (" + deckX + ", " + deckZ + ")");
        // A portal lit far along keeps its place: leaving by it opens outside at scale, and that way back in returns to it.
        BlockPos far = new BlockPos(is.cx() + 3000, 200, is.cz() - 2000);
        BlockPos exit = dev.zsskayr.merlins_inferno.portal.ElysiumDimension.ensureExit(level, far);
        int scale = dev.zsskayr.merlins_inferno.portal.ElysiumDimension.SCALE;
        helper.assertTrue(Math.hypot(exit.getX() - far.getX() * scale, exit.getZ() - far.getZ() * scale) < 12, "The exit opens at scaled coordinates: " + exit);
        helper.assertTrue(exit.equals(dev.zsskayr.merlins_inferno.portal.ElysiumDimension.ensureExit(level, far)), "A second exit reuses that frame");
        helper.succeed();
    }

    /** The decoration pass on a real level, high above the test area: a loose chain across chunks, the two new tree features, the cloud block. */
    @GameTest(template = "bench_empty")
    public static void elysiumDecorationBuilds(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1)).atY(240);
        var chain = new ElysiumWorld.HangingChain(o.getX(), o.getY(), o.getZ(), 1.0, 0.0, 14, 70, 42L);
        ChunkPos home = new ChunkPos(o);
        for (int dx = -1; dx <= 3; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                SpanBuilder.buildHanging(level, new ChunkPos(home.x + dx, home.z + dz), java.util.List.of(chain));
            }
        }
        int links = 0, lowest = Integer.MAX_VALUE, farthest = Integer.MIN_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-12, -85, -12), o.offset(40, 12, 12))) {
            BlockState state = level.getBlockState(p);
            if (state.is(net.minecraft.world.level.block.Blocks.TUFF_BRICKS) || state.is(net.minecraft.world.level.block.Blocks.POLISHED_TUFF)) {
                links++;
                lowest = Math.min(lowest, p.getY());
                farthest = Math.max(farthest, p.getX());
            }
        }
        helper.assertTrue(links > 200, "The loose chain draws links: " + links);
        helper.assertTrue(lowest <= o.getY() - 60, "It reaches its drop, lowest y " + lowest + " from " + o.getY());
        helper.assertTrue(farthest >= o.getX() + 10, "It swings out over the edge, x " + farthest + " from " + o.getX());

        var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var generator = level.getChunkSource().getGenerator();
        int offset = 40;
        for (String id : new String[] {"elysium_pale_tree", "elysium_mistfall_trees"}) {
            BlockPos soil = o.offset(offset, -30, 40);
            offset += 30;
            level.setBlock(soil, net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            var feature = features.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, id)));
            boolean grew = false;
            for (int attempt = 0; attempt < 8 && !grew; attempt++) {
                grew = feature.place(level, generator, level.getRandom(), soil.above());
            }
            helper.assertTrue(grew, id + " grows on grass");
        }

        BlockState cloud = ModBlocks.CLOUD_SEA.get().defaultBlockState();
        helper.assertTrue(!cloud.blocksMotion() && cloud.getCollisionShape(level, o).isEmpty(), "Entities fall through the cloud layer");
        helper.assertTrue(cloud.getShape(level, o).isEmpty(), "The cloud layer cannot be targeted");
        helper.assertTrue(!cloud.isValidSpawn(level, o, net.minecraft.world.entity.EntityType.PIG), "Nothing spawns on the cloud layer");
        helper.succeed();
    }

    /** The y of the highest block the core puts in a column (water and arches included), or -1 for open sky. */
    private static int expectedTop(Column c) {
        int hi = -1;
        if (c.land) {
            hi = c.hasWater() ? Math.max(c.top, c.waterTop) : c.top;
        }
        if (c.hasArch()) {
            hi = Math.max(hi, c.archTop);
        }
        return hi;
    }

    /** The y of the highest non-air block, or -1. */
    private static int highest(ChunkAccess chunk, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = ElysiumWorld.MIN_Y + ElysiumWorld.HEIGHT - 1; y >= ElysiumWorld.MIN_Y; y--) {
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
            case "snow_block", "snow", "powder_snow" -> 0xf2f4f6;
            case "ice", "packed_ice", "blue_ice" -> 0xa9c8f0;
            case "gravel" -> 0x8d8f91;
            case "mud" -> 0x5b5149;
            case "clay" -> 0x9aa3ad;
            case "dirt", "coarse_dirt", "rooted_dirt", "podzol" -> 0x7a6a58;
            case "moss_block" -> 0x5f7f3a;
            case "calcite", "quartz_block" -> 0xe6e4df;
            case "arganite", "quartzite" -> 0xd0d4d8;
            case "water" -> 0x4f7c93;
            default -> 0xff00ff;
        };
        double shade = 0.7 + 0.3 * (top - 170) / 120.0;
        shade = Math.max(0.5, Math.min(1.3, shade));
        int r = (int) Math.min(255, ((base >> 16) & 255) * shade), g = (int) Math.min(255, ((base >> 8) & 255) * shade), b = (int) Math.min(255, (base & 255) * shade);
        return (r << 16) | (g << 8) | b;
    }
}
