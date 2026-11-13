package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import dev.zsskayr.merlins_inferno.multiblock.HellForgeStructure.Layout;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * The Magmatic Core: a Hellforge that melts stone into the fuel it burns. It keeps itself hot with its own pool and makes a
 * little more than it uses. A Netherrack shell makes lava; a Blightsteel shell also makes Soul Lava.
 *
 * <p>Like Ebony Debris taking the look of the rock around it, the core takes the Blighted look ({@link #BLIGHTED}: a
 * desaturated frame) when it sits among more Blightsteel than Compressed Netherrack.
 */
public class MagmaticCoreBlock extends HellForgeCoreBlock {
    public static final BooleanProperty BLIGHTED = BooleanProperty.create("blighted");

    public MagmaticCoreBlock(Properties properties) {
        super(properties, 1, Layout.CUBE_9);
        this.registerDefaultState(this.defaultBlockState().setValue(BLIGHTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BLIGHTED);
    }

    @Override
    public boolean magmatic() {
        return true;
    }

    private static boolean blightedAround(BlockGetter level, BlockPos pos) {
        int blight = 0, nether = 0;
        for (Direction dir : Direction.values()) {
            BlockState around = level.getBlockState(pos.relative(dir));
            if (around.is(ModBlocks.BLIGHTSTEEL_CASING.get())) blight++;
            else if (around.is(ModBlocks.COMPRESSED_NETHERRACK.get())) nether++;
        }
        return blight > nether;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BLIGHTED, blightedAround(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return state.setValue(BLIGHTED, blightedAround(level, pos));
    }
}
