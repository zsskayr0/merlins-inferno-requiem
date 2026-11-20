package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.ArrayList;
import java.util.List;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModFluids;
import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;
import dev.zsskayr.merlins_inferno.worldgen.soul.SoulReservoirShape;
import dev.zsskayr.merlins_inferno.worldgen.soul.SoulAnchorChain;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Every chunk evaluates the same saved shape; it never writes into a neighbouring chunk. */
public final class SoulCalderaPiece extends StructurePiece {
    private final BlockPos center;
    private final SoulReservoirShape shape;
    private final List<SoulAnchorChain> chains;

    public SoulCalderaPiece(BlockPos center, SoulReservoirShape shape, List<SoulAnchorChain> chains) {
        super(ModStructurePieceTypes.SOUL_CALDERA.get(), 0, bounds(center, shape, chains));
        this.center = center;
        this.shape = shape;
        this.chains = List.copyOf(chains);
    }

    private static BoundingBox bounds(BlockPos center, SoulReservoirShape shape, List<SoulAnchorChain> chains) {
        BoundingBox box = new BoundingBox(
                center.getX() - shape.extentX(), center.getY() + shape.minY(), center.getZ() - shape.extentZ(),
                center.getX() + shape.extentX(), center.getY() + shape.maxY(), center.getZ() + shape.extentZ());
        for (var chain : chains) {
            box.encapsulate(new BoundingBox(Math.min(chain.ax(), chain.bx()) - 4, Math.min(chain.ay(), chain.by()) - 4,
                    Math.min(chain.az(), chain.bz()) - 4, Math.max(chain.ax(), chain.bx()) + 4,
                    Math.max(chain.ay(), chain.by()) + 4, Math.max(chain.az(), chain.bz()) + 4));
        }
        return box;
    }

    public SoulCalderaPiece(CompoundTag tag) {
        super(ModStructurePieceTypes.SOUL_CALDERA.get(), tag);
        center = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        shape = new SoulReservoirShape(tag.getInt("RX"), tag.getInt("RZ"), tag.getInt("Depth"),
                tag.getInt("Thickness"), tag.getLong("ShapeSeed"));
        chains = new ArrayList<>();
        int[] anchors = tag.getIntArray("Anchors");
        for (int i = 0; i + 5 < anchors.length; i += 6) {
            chains.add(new SoulAnchorChain(anchors[i], anchors[i + 1], anchors[i + 2], anchors[i + 3], anchors[i + 4], anchors[i + 5]));
        }
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", center.getX());
        tag.putInt("CY", center.getY());
        tag.putInt("CZ", center.getZ());
        tag.putInt("RX", shape.radiusX());
        tag.putInt("RZ", shape.radiusZ());
        tag.putInt("Depth", shape.depth());
        tag.putInt("Thickness", shape.thickness());
        tag.putLong("ShapeSeed", shape.seed());
        int[] anchors = new int[chains.size() * 6];
        for (int i = 0; i < chains.size(); i++) {
            var c = chains.get(i);
            anchors[i * 6] = c.ax(); anchors[i * 6 + 1] = c.ay(); anchors[i * 6 + 2] = c.az();
            anchors[i * 6 + 3] = c.bx(); anchors[i * 6 + 4] = c.by(); anchors[i * 6 + 5] = c.bz();
        }
        tag.putIntArray("Anchors", anchors);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos origin) {
        if (!level.getLevel().dimension().equals(OblivionDimension.LEVEL)) return;
        int minX = Math.max(chunkBox.minX(), boundingBox.minX()), maxX = Math.min(chunkBox.maxX(), boundingBox.maxX());
        int minZ = Math.max(chunkBox.minZ(), boundingBox.minZ()), maxZ = Math.min(chunkBox.maxZ(), boundingBox.maxZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        var lava = ModFluids.SOUL_LAVA_BLOCK.get().defaultBlockState();
        var magma = ModBlocks.SOUL_MAGMA.get().defaultBlockState();
        var basalt = Blocks.BASALT.defaultBlockState();
        var blackstone = Blocks.BLACKSTONE.defaultBlockState();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            var column = shape.column(x - center.getX(), z - center.getZ());
            if (column == null) continue;
            for (int dy = column.bottom(); dy <= shape.maxY(); dy++) {
                pos.set(x, center.getY() + dy, z);
                if (!chunkBox.isInside(pos)) continue;
                var material = shape.material(column, dy);
                var state = switch (material) {
                    case LAVA -> lava;
                    case MAGMA -> magma;
                    case ROCK -> dy < column.floor() - 2 || ((x >> 2) + (z >> 2)) % 4 == 0 ? basalt : blackstone;
                    case AIR -> Blocks.AIR.defaultBlockState();
                    case OUTSIDE -> null;
                };
                if (state != null) level.setBlock(pos, state, 2);
            }
        }
        for (var chain : chains) {
            chain.forEachBlock((x, y, z) -> {
                BlockPos p = new BlockPos(x, y, z);
                if (chunkBox.isInside(p)) level.setBlock(p, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
            });
            anchor(level, chunkBox, new BlockPos(chain.ax(), chain.ay(), chain.az()), false);
            anchor(level, chunkBox, new BlockPos(chain.bx(), chain.by(), chain.bz()), true);
        }
    }

    private static void anchor(WorldGenLevel level, BoundingBox chunkBox, BlockPos center, boolean ground) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            for (int dy = -2; dy <= (ground ? 3 : 2); dy++) {
                if (ground && dy > 1 && (Math.abs(dx) > 1 || Math.abs(dz) > 1)) continue;
                BlockPos p = center.offset(dx, dy, dz);
                if (chunkBox.isInside(p)) level.setBlock(p,
                        (dy == 1 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState(), 2);
            }
        }
    }
}
