package dev.zsskayr.merlins_inferno.block;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.HellForgePartBlockEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * The Hell Forge. {@code EntityBlock} purely to carry {@link HellForgeBlockEntity}, which exists
 * purely to carry a {@code BlockEntityRenderer} for {@code HellForgeModel} - its geometry spans
 * well past one block's 0-16 space (it's a ~2.5x2.5x2 block-sized structure), so it can't be a
 * normal static block model.
 * <p>
 * This block is placed at the structure's center cell, with {@link #FACING} pointing towards
 * whoever placed it (like a furnace). On placement it also fills in a real collision footprint
 * with invisible {@link HellForgePartBlock}s (see {@link #partOffsets}): a 3-wide, 2-tall solid
 * block across this block's own row and the row towards {@link #FACING}, so the model can't be
 * walked through, while the row opposite {@link #FACING} is left with no collision at all so a
 * hopper can be slotted in without fighting invisible blocks. Breaking either this block or any
 * part removes the whole structure - see {@link HellForgePartBlock}.
 */
public class HellForgeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<HellForgeBlock> CODEC = simpleCodec(HellForgeBlock::new);

    public HellForgeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HellForgeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (BlockPos offset : partOffsets(state)) {
            BlockPos partPos = pos.offset(offset);
            if (!level.getBlockState(partPos).canBeReplaced(context)) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            for (BlockPos offset : partOffsets(state)) {
                BlockPos partPos = pos.offset(offset);
                level.setBlock(partPos, ModBlocks.HELL_FORGE_PART.get().defaultBlockState(), 3);
                if (level.getBlockEntity(partPos) instanceof HellForgePartBlockEntity partBe) {
                    partBe.setCorePos(pos);
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && state.getBlock() != newState.getBlock() && !movedByPiston) {
            // Pass the OLD state directly rather than re-reading level.getBlockState(pos) - by this
            // point the chunk has already swapped in newState (air), which has no FACING property,
            // so re-deriving offsets from a fresh lookup here would throw/read the wrong facing and
            // leave some parts stuck behind (the ghost hitboxes seen after breaking/replacing).
            HellForgePartBlock.removeSurroundingParts(level, pos, state);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HellForgeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof HellForgeBlockEntity hellForge) {
            player.openMenu((MenuProvider) hellForge);
        }
        return InteractionResult.CONSUME;
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

    /**
     * The 11 positions (relative to this block) that get an invisible collision filler: the row one
     * step towards {@code FACING} and this block's own row, both 3 wide and 2 tall, minus this
     * block's own cell. The row one step opposite {@code FACING} is deliberately left out entirely -
     * no collision there, so a hopper can attach cleanly.
     */
    static List<BlockPos> partOffsets(BlockState coreState) {
        Direction forward = coreState.getValue(FACING);
        Direction right = forward.getClockWise();
        List<BlockPos> offsets = new ArrayList<>(11);
        for (int row = 0; row <= 1; row++) { // 0 = this block's own row, 1 = the row towards FACING
            for (int col = -1; col <= 1; col++) {
                for (int height = 0; height <= 1; height++) {
                    if (row == 0 && col == 0 && height == 0) {
                        continue; // this block's own cell
                    }
                    BlockPos offset = new BlockPos(
                            forward.getStepX() * row + right.getStepX() * col,
                            height,
                            forward.getStepZ() * row + right.getStepZ() * col);
                    offsets.add(offset);
                }
            }
        }
        return offsets;
    }
}
