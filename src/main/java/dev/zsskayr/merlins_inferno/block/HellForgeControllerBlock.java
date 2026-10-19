package dev.zsskayr.merlins_inferno.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.multiblock.HellForgeStructure;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;

/**
 * The Hell Forge's Controller: the one block of the 3x3x3 structure (see
 * {@link dev.zsskayr.merlins_inferno.multiblock.HellForgeStructure}) that opens the GUI and holds
 * the inventory. It goes at the centre of the structure's front face, {@link #FACING} outwards
 * like a furnace. The GUI opens only when the structure is formed; otherwise the player gets an
 * action-bar message saying which block is wrong. Its only visual state is {@link #ACTIVE}, which
 * animates the front face while the forge cooks; the rest of the structure shows nothing.
 */
public class HellForgeControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<HellForgeControllerBlock> CODEC = simpleCodec(HellForgeControllerBlock::new);
    /** True while the forge is cooking: switches the front face to its animated texture. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public HellForgeControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends HellForgeControllerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Nullable
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HellForgeBlockEntity(pos, state);
    }

    /** The only place a Hell Forge GUI is opened - and only once the structure checks out. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof HellForgeBlockEntity hellForge) {
            hellForge.refreshStructure();
            if (hellForge.isFormed()) {
                player.openMenu((MenuProvider) hellForge);
            } else {
                player.displayClientMessage(hellForge.getStructureError(), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** Sets the Core in the structure's centre active/inactive (no-op if it is not there or already so). */
    public static void setCoreActive(Level level, BlockPos controllerPos, BlockState controllerState, boolean active) {
        BlockPos corePos = HellForgeStructure.corePos(controllerPos, controllerState.getValue(FACING));
        BlockState core = level.getBlockState(corePos);
        if (core.getBlock() instanceof HellForgeCoreBlock && core.getValue(HellForgeCoreBlock.ACTIVE) != active) {
            level.setBlock(corePos, core.setValue(HellForgeCoreBlock.ACTIVE, active), 3);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel
                && level.getBlockEntity(pos) instanceof HellForgeBlockEntity hellForge) {
            // Must happen before super.onRemove() - that call is what actually discards the block
            // entity, so the input/fuel/result slots would otherwise vanish with it.
            Containers.dropContents(level, pos, hellForge);
        }
        if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
            // Without its Controller the Core would be left animating forever.
            setCoreActive(level, pos, state, false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != ModBlockEntityTypes.HELL_FORGE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<HellForgeBlockEntity>) HellForgeBlockEntity::serverTick;
    }
}
