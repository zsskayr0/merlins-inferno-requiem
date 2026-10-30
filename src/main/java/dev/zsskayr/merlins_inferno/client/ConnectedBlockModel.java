package dev.zsskayr.merlins_inferno.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import dev.zsskayr.merlins_inferno.compat.ae2.Ae2FacadeCompat;
import dev.zsskayr.merlins_inferno.compat.framedblocks.FramedBlocksCompat;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Connected textures for a full block. Every face reads a 32x24 sheet of 16 px tiles holding the 47 distinct looks a face
 * can have, 16 variants of each, picked by which of its four sides touch another block of the same kind and, for the corners where two
 * touching sides meet, whether the diagonal block is there too (a missing diagonal leaves a notch in the corner).
 *
 * <p>The key is {@code L | R<<1 | U<<2 | D<<3 | UL<<4 | UR<<5 | DL<<6 | DR<<7}, "left" and "up" being as the face's own
 * texture sees them. A diagonal only counts when both of its sides do, which folds the 256 keys into 47; the tile index
 * is the rank of the folded key, and tools/gen_polished_blightstone.py builds the sheet in the same order. The variant
 * (x mod 4 + 4 * y mod 4, along the face's own right and down) is a different window of one 64x64 face pattern, so the
 * face does not repeat for four blocks and neighbouring blocks still line up.
 *
 * <p>The whole 3x3x3 neighbourhood is collected per block in {@link #getModelData}; the key is built per quad from the
 * quad's own geometry (which way its U and V run in the world), so it works for any face. The item model has no
 * ModelData and shows the plain framed tile.
 *
 * <p>In {@code pairs} mode (Tiled Porcelain) a block only counts as connected to a neighbour that shares its 2x2 group,
 * the groups being the first/second halves counted along each axis from where a line of blocks starts, so a flat
 * area reads as big two-by-two tiles with a border around each one; a lone block is a fully framed tile.
 */
public final class ConnectedBlockModel extends BakedModelWrapper<BakedModel> {
    /** Bit {@code (dx+1) + 3*(dy+1) + 9*(dz+1)}: the block at that offset is the same block. */
    public static final ModelProperty<Integer> NEIGHBOURS = new ModelProperty<>();
    private static final ModelProperty<BlockPos> POS = new ModelProperty<>();
    /** How far the start of a line of Tiled Porcelain is searched for; a longer line pairs up from here. */
    private static final int MAX_RUN = 128;
    private static final int VERTEX_SIZE = 8; // DefaultVertexFormat.BLOCK, in ints
    private static final int UV_OFFSET = 4;
    private static final int GRID_W = 32, GRID_H = 24, VARIANTS = 16;
    /** Folded key -> tile index; -1 for keys that are not folded. */
    private static final int[] TILE_OF_KEY = new int[256];

    static {
        int next = 0;
        for (int key = 0; key < 256; key++) {
            TILE_OF_KEY[key] = fold(key) == key ? next++ : -1;
        }
    }

    private final TextureAtlasSprite sheet;
    /** When set, only the quads that use this sprite are remapped (a pane's edge keeps its own texture). */
    private final @Nullable TextureAtlasSprite only;
    private final boolean pairs;

    public ConnectedBlockModel(BakedModel original, TextureAtlasSprite sheet) {
        this(original, sheet, null, false);
    }

    public ConnectedBlockModel(BakedModel original, TextureAtlasSprite sheet, @Nullable TextureAtlasSprite only) {
        this(original, sheet, only, false);
    }

    public ConnectedBlockModel(BakedModel original, TextureAtlasSprite sheet, @Nullable TextureAtlasSprite only, boolean pairs) {
        super(original);
        this.sheet = sheet;
        this.only = only;
        this.pairs = pairs;
    }

    /** Drops the diagonal bits whose two sides are not both connected. */
    static int fold(int key) {
        int out = key & 15;
        if ((key & 1) != 0 && (key & 4) != 0) out |= key & 16;   // UL needs L and U
        if ((key & 2) != 0 && (key & 4) != 0) out |= key & 32;   // UR needs R and U
        if ((key & 1) != 0 && (key & 8) != 0) out |= key & 64;   // DL needs L and D
        if ((key & 2) != 0 && (key & 8) != 0) out |= key & 128;  // DR needs R and D
        return out;
    }

    /** The block a Framed Blocks block or an AE2 facade shows; anything else is itself. */
    private static BlockState camouflaged(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return Ae2FacadeCompat.resolve(level, pos, FramedBlocksCompat.resolve(level, pos, state));
    }

    private static int bit(int dx, int dy, int dz) {
        return 1 << ((dx + 1) + 3 * (dy + 1) + 9 * (dz + 1));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state,
                                           @NotNull ModelData data) {
        int bits = 0;
        // Framed Blocks (and similar) hand us their own state; what matters is the block they are camouflaged as.
        BlockState self = camouflaged(level, pos, state);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx | dy | dz) == 0) {
                        continue;
                    }
                    at.setWithOffset(pos, dx, dy, dz);
                    BlockState other = camouflaged(level, at, level.getBlockState(at));
                    if (other.is(self.getBlock()) && (!pairs || sameGroup(level, pos, at, self, dx, dy, dz))) {
                        bits |= bit(dx, dy, dz);
                    }
                }
            }
        }
        return super.getModelData(level, pos, state, data).derive().with(NEIGHBOURS, bits).with(POS, pos.immutable()).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        Integer bits = data.get(NEIGHBOURS);
        BlockPos pos = data.get(POS);
        if (pos == null) {
            pos = BlockPos.ZERO; // Framed Blocks only forwards NEIGHBOURS and caches by it, so there is no per-block variant
        }
        if (bits == null || bits == 0 || quads.isEmpty()) {
            return quads;
        }
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            out.add(only == null || quad.getSprite() == only ? remap(quad, bits, pos) : quad);
        }
        return out;
    }

    private BakedQuad remap(BakedQuad quad, int neighbours, BlockPos pos) {
        int[] v = quad.getVertices().clone();
        TextureAtlasSprite sprite = quad.getSprite();
        float uSpan = sprite.getU1() - sprite.getU0();
        float vSpan = sprite.getV1() - sprite.getV0();

        float[] relU = new float[4], relV = new float[4], x = new float[4], y = new float[4], z = new float[4];
        for (int i = 0; i < 4; i++) {
            int o = i * VERTEX_SIZE;
            x[i] = Float.intBitsToFloat(v[o]);
            y[i] = Float.intBitsToFloat(v[o + 1]);
            z[i] = Float.intBitsToFloat(v[o + 2]);
            relU[i] = (Float.intBitsToFloat(v[o + UV_OFFSET]) - sprite.getU0()) / uSpan;
            relV[i] = (Float.intBitsToFloat(v[o + UV_OFFSET + 1]) - sprite.getV0()) / vSpan;
        }
        // The world directions the texture's U (right) and V (down) run towards on this quad: the gradient of U and V
        // over all four corners. (Comparing just the lowest and highest corner is wrong whenever those two happen to be
        // diagonal, which depends on the quad's vertex order and picked the wrong axis on some faces.)
        float meanU = (relU[0] + relU[1] + relU[2] + relU[3]) / 4f, meanV = (relV[0] + relV[1] + relV[2] + relV[3]) / 4f;
        float ux = 0, uy = 0, uz = 0, vx = 0, vy = 0, vz = 0;
        for (int i = 0; i < 4; i++) {
            ux += (relU[i] - meanU) * x[i];
            uy += (relU[i] - meanU) * y[i];
            uz += (relU[i] - meanU) * z[i];
            vx += (relV[i] - meanV) * x[i];
            vy += (relV[i] - meanV) * y[i];
            vz += (relV[i] - meanV) * z[i];
        }
        Direction right = along(ux, uy, uz);
        Direction down = along(vx, vy, vz);

        int key = (at(neighbours, pos, right, down, -1, 0) ? 1 : 0)
                | (at(neighbours, pos, right, down, 1, 0) ? 2 : 0)
                | (at(neighbours, pos, right, down, 0, -1) ? 4 : 0)
                | (at(neighbours, pos, right, down, 0, 1) ? 8 : 0)
                | (at(neighbours, pos, right, down, -1, -1) ? 16 : 0)
                | (at(neighbours, pos, right, down, 1, -1) ? 32 : 0)
                | (at(neighbours, pos, right, down, -1, 1) ? 64 : 0)
                | (at(neighbours, pos, right, down, 1, 1) ? 128 : 0);
        int tile = TILE_OF_KEY[fold(key)];
        // Which window of the face pattern: the block's place along the face's own right and down, wrapping every 4.
        int across = Math.floorMod(pos.getX() * right.getStepX() + pos.getY() * right.getStepY() + pos.getZ() * right.getStepZ(), 4);
        int downward = Math.floorMod(pos.getX() * down.getStepX() + pos.getY() * down.getStepY() + pos.getZ() * down.getStepZ(), 4);
        int index = tile * VARIANTS + across + 4 * downward;
        int col = index % GRID_W, row = index / GRID_W;

        float su0 = sheet.getU0(), su1 = sheet.getU1(), sv0 = sheet.getV0(), sv1 = sheet.getV1();
        for (int i = 0; i < 4; i++) {
            int o = i * VERTEX_SIZE + UV_OFFSET;
            v[o] = Float.floatToRawIntBits(su0 + (su1 - su0) * ((col + relU[i]) / GRID_W));
            v[o + 1] = Float.floatToRawIntBits(sv0 + (sv1 - sv0) * ((row + relV[i]) / GRID_H));
        }
        return new BakedQuad(v, quad.getTintIndex(), quad.getDirection(), sheet, quad.isShade(), quad.hasAmbientOcclusion());
    }

    /**
     * Whether the block {@code r} steps along the face's right and {@code d} steps along its down counts as connected:
     * it is the same block and, in pairs mode, lies in the same 2x2 group as this one.
     */
    private boolean at(int neighbours, BlockPos pos, Direction right, Direction down, int r, int d) {
        int dx = right.getStepX() * r + down.getStepX() * d;
        int dy = right.getStepY() * r + down.getStepY() * d;
        int dz = right.getStepZ() * r + down.getStepZ() * d;
        if ((neighbours & bit(dx, dy, dz)) == 0) {
            return false;
        }
        return true;
    }

    /**
     * Whether two Tiled Porcelain blocks, {@code (dx, dy, dz)} apart, are halves of the same 2x2 group. Nothing is stored in
     * the blocks: along each axis a block is the first or second half of a pair counted from where its unbroken line of
     * Tiled Porcelain starts, so a row builds tiles from its low end however it was placed. Across an axis they differ on, this
     * one must be the first half going up (the second going down); on an axis they share, both must be the same half.
     */
    private static boolean sameGroup(BlockAndTintGetter level, BlockPos pos, BlockPos other, BlockState self, int dx, int dy, int dz) {
        return sameHalf(level, pos, other, self, Direction.Axis.X, dx) && sameHalf(level, pos, other, self, Direction.Axis.Y, dy)
                && sameHalf(level, pos, other, self, Direction.Axis.Z, dz);
    }

    private static boolean sameHalf(BlockAndTintGetter level, BlockPos pos, BlockPos other, BlockState self, Direction.Axis axis, int d) {
        boolean second = secondHalf(level, pos, self, axis);
        boolean otherSecond = secondHalf(level, other, self, axis);
        return d == 0 ? second == otherSecond : (second == (d < 0) && otherSecond != second);
    }

    /** Whether the block is the second half of its pair along the axis: an odd number of blocks above the start of its line. */
    private static boolean secondHalf(BlockAndTintGetter level, BlockPos pos, BlockState self, Direction.Axis axis) {
        Direction down = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        BlockPos.MutableBlockPos at = pos.mutable();
        int above = 0;
        while (above < MAX_RUN) {
            at.move(down);
            if (!camouflaged(level, at, level.getBlockState(at)).is(self.getBlock())) {
                break;
            }
            above++;
        }
        return (above & 1) == 1;
    }

    /** The axis direction a vector mostly points along. */
    private static Direction along(float dx, float dy, float dz) {
        float ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax >= ay && ax >= az) return dx >= 0 ? Direction.EAST : Direction.WEST;
        if (ay >= az) return dy >= 0 ? Direction.UP : Direction.DOWN;
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
