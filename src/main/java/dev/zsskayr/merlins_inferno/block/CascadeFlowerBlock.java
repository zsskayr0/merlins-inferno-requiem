package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Decorative flowers rooted in the Cascade Vale's grass. */
public class CascadeFlowerBlock extends BushBlock {
    public static final MapCodec<CascadeFlowerBlock> CODEC = simpleCodec(CascadeFlowerBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public CascadeFlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CascadeFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
