package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.neoforged.neoforge.common.IShearable;

/** Two-block grass for Cascade Vale, with vanilla double-plant placement and survival behavior. */
public class TallPinkGrassBlock extends DoublePlantBlock implements IShearable {
    public static final MapCodec<TallPinkGrassBlock> CODEC = simpleCodec(TallPinkGrassBlock::new);

    public TallPinkGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TallPinkGrassBlock> codec() {
        return CODEC;
    }
}
