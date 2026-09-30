package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
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
 * A wooden rack that dries Raw Edenweed Buds into Dried Edenweed (four at a time, see {@link DryingRackBlockEntity}).
 * Right-click with a bud to hang it; right-click with an empty hand to take what has dried. The {@link #CONTENTS}
 * property only drives which model is shown.
 */
public class DryingRackBlock extends BaseEntityBlock {
    public static final MapCodec<DryingRackBlock> CODEC = simpleCodec(DryingRackBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Contents> CONTENTS = EnumProperty.create("contents", Contents.class);

    // Two posts and a crossbar; the bar runs east-west when facing north/south.
    private static final VoxelShape SHAPE_X = Shapes.or(Block.box(0, 0, 7, 2, 16, 9), Block.box(14, 0, 7, 16, 16, 9), Block.box(0, 13, 7, 16, 16, 9));
    private static final VoxelShape SHAPE_Z = Shapes.or(Block.box(7, 0, 0, 9, 16, 2), Block.box(7, 0, 14, 9, 16, 16), Block.box(7, 13, 0, 9, 16, 16));

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
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONTENTS, Contents.EMPTY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONTENTS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_X : SHAPE_Z;
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
        if (!stack.is(ModItems.RAW_EDENWEED_BUD.get())) {
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
