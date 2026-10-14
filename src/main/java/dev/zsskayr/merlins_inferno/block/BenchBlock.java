package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;
import dev.zsskayr.merlins_inferno.entity.BenchSeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** One seat per block; only benches facing the same way share their end pieces. */
public class BenchBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<BenchBlock> CODEC = simpleCodec(BenchBlock::new);
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final double SEAT_HEIGHT = 0.5;
    private static final double HEIGHT_SCALE = 32.0 / 27.0;
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][4];

    static {
        for (int connections = 0; connections < 4; connections++) {
            VoxelShape base = Shapes.or(Block.box(0, 5.25, 3.625, 16, 6.75, 15),
                    Block.box(0, 7.5, 1.875, 16, 16.5, 2.75));
            if ((connections & 1) == 0) base = Shapes.or(base,
                    Block.box(0, 0, 1, 1.3, 17.25, 3.625),
                    Block.box(0.2, 0, 12.375, 1.1, 11.25, 15),
                    Block.box(0.2, 6, 3.625, 1.1, 11.25, 15));
            if ((connections & 2) == 0) base = Shapes.or(base,
                    Block.box(14.7, 0, 1, 16, 17.25, 3.625),
                    Block.box(14.9, 0, 12.375, 15.8, 11.25, 15),
                    Block.box(14.9, 6, 3.625, 15.8, 11.25, 15));
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                VoxelShape rotated = Shapes.empty();
                for (net.minecraft.world.phys.AABB box : base.toAabbs()) {
                    double x0=box.minX, x1=box.maxX, z0=1-box.maxZ, z1=1-box.minZ;
                    int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
                    for (int i=0; i<turns; i++) { double nx0=1-z1, nx1=1-z0; z0=x0; z1=x1; x0=nx0; x1=nx1; }
                    rotated = Shapes.or(rotated, Shapes.box(x0,box.minY * HEIGHT_SCALE,z0,x1,box.maxY * HEIGHT_SCALE,z1));
                }
                SHAPES[facing.get2DDataValue()][connections] = rotated.optimize();
            }
        }
    }

    public BenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(LEFT, false).setValue(RIGHT, false).setValue(WATERLOGGED, false));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT, WATERLOGGED);
    }
    private boolean connects(BlockState state, BlockState neighbor) {
        return neighbor.is(this) && neighbor.getValue(FACING) == state.getValue(FACING);
    }
    private BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        // LEFT is the negative X end in the north-facing model.
        return state.setValue(LEFT, connects(state, level.getBlockState(pos.relative(facing.getCounterClockWise()))))
                .setValue(RIGHT, connects(state, level.getBlockState(pos.relative(facing.getClockWise()))));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER),
                context.getLevel(), context.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return connections(state, level, pos);
    }
    @Override protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()][(state.getValue(LEFT)?1:0)|(state.getValue(RIGHT)?2:0)];
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
                .setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
            BlockHitResult hit) {
        // Holding another segment should place it instead of sitting on the clicked segment.
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() == this)
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.isPassenger()) return InteractionResult.PASS;
        if (!level.isClientSide) BenchSeatEntity.sit(level, pos, state.getValue(FACING), player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide) BenchSeatEntity.removeAt(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
    }
}
