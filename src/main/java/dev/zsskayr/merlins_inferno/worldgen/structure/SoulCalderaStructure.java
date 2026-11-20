package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModStructureTypes;
import dev.zsskayr.merlins_inferno.worldgen.soul.SoulReservoirShape;
import dev.zsskayr.merlins_inferno.worldgen.soul.SoulAnchorChain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Large, contained soul-lava lakes on suspended basalt islands, only in Oblivion's open sky. */
public final class SoulCalderaStructure extends Structure {
    public static final MapCodec<SoulCalderaStructure> CODEC = simpleCodec(SoulCalderaStructure::new);
    private static final int CLEARANCE = 18;

    public SoulCalderaStructure(StructureSettings settings) { super(settings); }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!(context.chunkGenerator() instanceof NoiseBasedChunkGenerator noise)
                || !noise.generatorSettings().is(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "oblivion"))) {
            return Optional.empty();
        }
        var random = context.random();
        int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
        var shape = new SoulReservoirShape(32 + random.nextInt(15), 30 + random.nextInt(15),
                7 + random.nextInt(5), 26 + random.nextInt(10), random.nextLong());
        int lakeY = 208 + random.nextInt(28);
        // Sampling the whole footprint before accepting the start avoids per-chunk acceptance seams.
        // A generous air gap also leaves room for normal surface decoration beneath the island.
        for (int dx = -shape.extentX(); dx <= shape.extentX(); dx += 4) {
            for (int dz = -shape.extentZ(); dz <= shape.extentZ(); dz += 4) {
                var column = shape.column(dx, dz);
                if (column == null) continue;
                int terrain = noise.getBaseHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                lakeY = Math.max(lakeY, terrain + CLEARANCE - column.bottom());
            }
        }
        if (lakeY + shape.maxY() >= context.heightAccessor().getMaxBuildHeight() - 4) return Optional.empty();
        BlockPos center = new BlockPos(x, lakeY, z);
        List<SoulAnchorChain> chains = anchors(context, center, shape);
        // Never leave an unanchored island when the surrounding terrain cannot hold the tethers.
        if (chains.size() < 3) return Optional.empty();
        return Optional.of(new GenerationStub(center,
                builder -> builder.addPiece(new SoulCalderaPiece(center, shape, chains))));
    }

    private static List<SoulAnchorChain> anchors(GenerationContext context, BlockPos center, SoulReservoirShape shape) {
        List<SoulAnchorChain> result = new ArrayList<>();
        double phase = (shape.seed() & 65535L) * (Math.PI * 2 / 65536.0);
        // Four sectors prevent all successful chains from ending up on the same side.
        for (int sector = 0; sector < 4; sector++) {
            for (int attempt = 0; attempt < 3; attempt++) {
                double angle = phase + sector * Math.PI / 2 + (attempt - 1) * 0.22;
                double ux = Math.cos(angle), uz = Math.sin(angle);
                int dx = 0, dz = 0;
                SoulReservoirShape.Column column = null;
                for (double r = 0.72; r <= 1.20; r += 0.025) {
                    dx = (int) Math.round(ux * shape.radiusX() * r);
                    dz = (int) Math.round(uz * shape.radiusZ() * r);
                    column = shape.column(dx, dz);
                    if (column != null && column.radius() >= 0.78 && column.radius() <= 0.86) break;
                    column = null;
                }
                if (column == null) continue;
                int ax = center.getX() + dx, az = center.getZ() + dz;
                int ay = center.getY() + column.bottom() + 4;
                int bx = ax + (int) Math.round(ux * 26), bz = az + (int) Math.round(uz * 26);
                int ground = context.chunkGenerator().getBaseHeight(bx, bz, Heightmap.Types.OCEAN_FLOOR_WG,
                        context.heightAccessor(), context.randomState()) - 1;
                if (ground < context.chunkGenerator().getSeaLevel() + 3 || ground > ay - 24) continue;
                boolean supported = true;
                // The footing is buried into a 5x5 solid area, never suspended above lava or a cave.
                for (int ox = -2; ox <= 2 && supported; ox++) for (int oz = -2; oz <= 2; oz++) {
                    var base = context.chunkGenerator().getBaseColumn(bx + ox, bz + oz,
                            context.heightAccessor(), context.randomState());
                    var support = base.getBlock(ground - 3);
                    if (support.isAir() || !support.getFluidState().isEmpty()
                            || !base.getBlock(ground + 4).isAir()) { supported = false; break; }
                }
                if (!supported) continue;
                // Reject a tether whose exposed span would be swallowed by an intervening ridge.
                for (double f = 0.12; f < 0.90 && supported; f += 0.10) {
                    int px = (int) Math.round(ax + (bx - ax) * f);
                    int pz = (int) Math.round(az + (bz - az) * f);
                    int terrain = context.chunkGenerator().getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG,
                            context.heightAccessor(), context.randomState());
                    if (terrain > ay + (ground - ay) * f - 3) supported = false;
                }
                if (supported) {
                    result.add(new SoulAnchorChain(ax, ay, az, bx, ground - 1, bz));
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public StructureType<?> type() { return ModStructureTypes.SOUL_CALDERA.get(); }
}
