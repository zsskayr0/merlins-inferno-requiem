package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.IShearable;

/** Recolored vanilla short grass for the Cascade Vale. */
public class PinkGrassBlock extends BushBlock implements IShearable {
    public static final MapCodec<PinkGrassBlock> CODEC = simpleCodec(PinkGrassBlock::new);
    // Matches vanilla short grass (TallGrassBlock), including its selection height.
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

    public PinkGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PinkGrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
