package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.zsskayr.merlins_inferno.blockentity.DryingRackBlockEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * A rack that dries Raw Edenweed into Dried Edenweed (four at a time, see {@link DryingRackBlockEntity}). It sticks
 * to a wall (a shallow shelf) or hangs from a ceiling (a rope-and-bar hanger while drying, a shelf while empty or
 * ready). Right-click with edenweed to hang it; right-click with an empty hand to take what has dried. {@link #FACE}
 * and {@link #CONTENTS} only drive which model is shown.
 */
public class DryingRackBlock extends BaseEntityBlock {
    public static final MapCodec<DryingRackBlock> CODEC = simpleCodec(DryingRackBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final EnumProperty<Contents> CONTENTS = EnumProperty.create("contents", Contents.class);

    // A shallow shelf hugging the wall behind it (the wall is on the FACING side).
    private static final VoxelShape SHAPE_WALL_NORTH = Block.box(0, 9, 13, 16, 16, 16);
    private static final VoxelShape SHAPE_WALL_SOUTH = Block.box(0, 9, 0, 16, 16, 3);
    private static final VoxelShape SHAPE_WALL_WEST = Block.box(13, 9, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_WALL_EAST = Block.box(0, 9, 0, 3, 16, 16);
    // A hanger dropping from the ceiling above.
    private static final VoxelShape SHAPE_CEILING = Shapes.or(Block.box(6, 13, 6, 10, 16, 10), Block.box(1, 4, 6, 15, 8, 10));

    public enum Contents implements StringRepresentable {
        EMPTY("empty"), DRYING("drying"), READY("ready");

        private final String name;

        Contents(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public DryingRackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL).setValue(CONTENTS, Contents.EMPTY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, CONTENTS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState state;
            if (direction == Direction.UP) {
                state = this.defaultBlockState().setValue(FACE, AttachFace.CEILING)
                        .setValue(FACING, context.getHorizontalDirection());
            } else if (direction == Direction.DOWN) {
                continue;
            } else {
                state = this.defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, direction);
            }
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(FACE) == AttachFace.CEILING) {
            return SHAPE_CEILING;
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_WALL_SOUTH;
            case WEST -> SHAPE_WALL_WEST;
            case EAST -> SHAPE_WALL_EAST;
            default -> SHAPE_WALL_NORTH;
        };
    }

    private static BlockPos supportPos(BlockPos pos, BlockState state) {
        return state.getValue(FACE) == AttachFace.CEILING ? pos.above() : pos.relative(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = supportPos(pos, state);
        Direction supportFace = state.getValue(FACE) == AttachFace.CEILING ? Direction.DOWN : state.getValue(FACING).getOpposite();
        return level.getBlockState(support).isFaceSturdy(level, support, supportFace);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return neighborPos.equals(supportPos(pos, state)) && !this.canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntityTypes.DRYING_RACK.get(), DryingRackBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.RAW_EDENWEED.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack && rack.hang()) {
            if (!level.isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                rack.refreshState();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
            int taken = level.isClientSide ? (rack.hasReady() ? 1 : 0) : rack.takeDried(player);
            if (taken > 0) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack && !level.isClientSide) {
            for (ItemStack stack : rack.dropContents()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
