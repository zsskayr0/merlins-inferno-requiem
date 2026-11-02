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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gives every face of a Gate its own rune. The rune sprite is an animated strip of 64x64 frames holding a 4x4 grid of 16 px
 * tiles (tools/gen_gates.py), each tile a rune of its own that fades out and in and changes at its own pace; the quads of
 * the overlay show the tile picked from the block's position and the face, so neighbouring faces never twinkle together and
 * a whole wall of gates reads as a field of blinking stars. Must wrap the {@link ConnectedBlockModel}, whose model data
 * carries the position; the item (no position) shows the first tile.
 */
public final class GateRuneModel extends BakedModelWrapper<BakedModel> {
    private static final int GRID = 4;
    private final TextureAtlasSprite runes;

    public GateRuneModel(BakedModel original, TextureAtlasSprite runes) {
        super(original);
        this.runes = runes;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (quads.isEmpty()) {
            return quads;
        }
        BlockPos pos = data.get(ConnectedBlockModel.POS);
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            out.add(quad.getSprite() == runes ? remap(quad, pos) : quad);
        }
        return out;
    }

    private BakedQuad remap(BakedQuad quad, @Nullable BlockPos pos) {
        int variant = pos == null ? 0 : Math.floorMod(pos.getX() * 7 + pos.getY() * 13 + pos.getZ() * 17
                + quad.getDirection().ordinal() * 5 + (pos.getX() ^ pos.getZ()) * 3, GRID * GRID);
        int col = variant % GRID, row = variant / GRID;
        int[] v = quad.getVertices().clone();
        float uSpan = runes.getU1() - runes.getU0();
        float vSpan = runes.getV1() - runes.getV0();
        for (int i = 0; i < 4; i++) {
            int o = i * 8 + 4;
            float relU = (Float.intBitsToFloat(v[o]) - runes.getU0()) / uSpan;
            float relV = (Float.intBitsToFloat(v[o + 1]) - runes.getV0()) / vSpan;
            v[o] = Float.floatToRawIntBits(runes.getU0() + uSpan * ((col + relU) / GRID));
            v[o + 1] = Float.floatToRawIntBits(runes.getV0() + vSpan * ((row + relV) / GRID));
        }
        return new BakedQuad(v, quad.getTintIndex(), quad.getDirection(), runes, quad.isShade(), quad.hasAmbientOcclusion());
    }
}
