package dev.zsskayr.merlins_inferno.block;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import dev.zsskayr.merlins_inferno.blockentity.SacredAltarPartBlockEntity;

/**
 * The invisible "filler" block that gives the Sacred Altar (see {@link SacredAltarBlock}) a real 3 x 3 x 2 hitbox - the same
 * scheme as {@link HellForgePartBlock}, and with the same care about the ghost-hitbox bugs that one went through:
 * <ul>
 *     <li>it is only ever placed by the altar ({@link SacredAltarBlock#placeParts}), never by hand, and has no item;</li>
 *     <li>clicking it does what clicking the altar does (the diamond, or the hint), whichever face is hit;</li>
 *     <li>breaking a part breaks the whole structure, and breaking the altar removes every part - {@link #TEARDOWN_IN_PROGRESS}
 *     keeps a cascade started from one side from running again from the other;</li>
 *     <li>the teardown lives in {@code onRemove}, never in {@code BlockEntity#setRemoved()}: that one also runs when a chunk
 *     unloads, where looking up the neighbouring blocks can deadlock the server thread;</li>
 *     <li>the part's own state is never re-read after the swap - the cascade is handed the positions it needs.</li>
 * </ul>
 * Like the altar it is unbreakable in survival and drops nothing.
 */
public class SacredAltarPartBlock extends Block implements EntityBlock {
    /** A {@link BlockPos} alone can't tell two dimensions' identical coordinates apart. */
    private record LevelPos(ResourceKey<Level> dimension, BlockPos pos) {
        static LevelPos of(Level level, BlockPos pos) {
            return new LevelPos(level.dimension(), pos.immutable());
        }
    }

    static final Set<LevelPos> TEARDOWN_IN_PROGRESS = new HashSet<>();

    public SacredAltarPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SacredAltarPartBlockEntity(pos, state);
    }

    @Nullable
    private static BlockPos corePos(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SacredAltarPartBlockEntity part ? part.getCorePos() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        BlockPos core = corePos(level, pos);
        if (core == null || !(level.getBlockState(core).getBlock() instanceof SacredAltarBlock)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return SacredAltarBlock.useOffering(stack, level, core, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return SacredAltarBlock.showHint(level, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock() && !movedByPiston) {
            BlockPos core = corePos(level, pos);
            if (core != null) {
                breakFromPart(level, core);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston); // removes this part's block entity
    }

    /** Removes every part around {@code corePos} (called from {@link SacredAltarBlock#onRemove}). */
    public static void removeSurroundingParts(Level level, BlockPos corePos) {
        LevelPos key = LevelPos.of(level, corePos);
        TEARDOWN_IN_PROGRESS.add(key);
        try {
            for (BlockPos offset : SacredAltarBlock.PART_OFFSETS) {
                BlockPos pos = corePos.offset(offset);
                if (level.getBlockState(pos).getBlock() instanceof SacredAltarPartBlock) {
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            TEARDOWN_IN_PROGRESS.remove(key);
        }
    }

    /** A part is the one being broken (creative mode, or a command): the altar goes, and its own removal clears the other parts. */
    private static void breakFromPart(Level level, BlockPos corePos) {
        if (TEARDOWN_IN_PROGRESS.contains(LevelPos.of(level, corePos))) {
            return; // the altar's own onRemove is already tearing everything down
        }
        if (level.getBlockState(corePos).getBlock() instanceof SacredAltarBlock) {
            level.removeBlock(corePos, false);
        }
    }
}
