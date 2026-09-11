package dev.zsskayr.merlins_inferno.block;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.HellForgePartBlockEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * The invisible "filler" block that gives the Hell Forge (see {@link HellForgeBlock}) real
 * collision across its footprint - it has no item and is never placed by hand, only ever by
 * {@link HellForgeBlock#setPlacedBy} alongside the real block. It renders nothing
 * ({@link RenderShape#INVISIBLE}) and keeps the default full-cube collision shape, so it simply
 * blocks movement like a normal solid block wherever it's placed.
 * <p>
 * Breaking a part breaks the whole structure (and drops one Hell Forge) via {@link #onRemove};
 * breaking the real block removes every part via {@link HellForgeBlock#onRemove}.
 * {@link #TEARDOWN_IN_PROGRESS} exists purely so a cascade started from one side doesn't also fire
 * (and double-drop) from the other.
 * <p>
 * The teardown check lives in {@code onRemove}, not {@code BlockEntity#setRemoved()} - the latter
 * also fires during a plain chunk unload (world save/shutdown), where checking "is this genuinely
 * gone" via {@code level.getBlockState(pos)} can deadlock the server thread (it may need to load a
 * neighboring chunk, which blocks waiting for the very thread that's currently unloading this one).
 * {@code onRemove} only ever fires for a real state change, so no such check is needed here.
 */
public class HellForgePartBlock extends Block implements EntityBlock {
    /** A {@link BlockPos} alone can't tell two dimensions' identical coordinates apart. */
    private record LevelPos(ResourceKey<Level> dimension, BlockPos pos) {
        static LevelPos of(Level level, BlockPos pos) {
            return new LevelPos(level.dimension(), pos.immutable());
        }
    }

    static final Set<LevelPos> TEARDOWN_IN_PROGRESS = new HashSet<>();

    /**
     * Positions a creative-mode player is in the middle of breaking. {@code playerWillDestroy}
     * fires before removal with the player available; {@code onRemove} fires during removal with
     * the drop logic but no player - this bridges the two so creative breaks give nothing, matching
     * how every normal (loot-table-driven) block already behaves in creative.
     */
    private static final Set<LevelPos> CREATIVE_BREAK = new HashSet<>();

    public HellForgePartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HellForgePartBlockEntity(pos, state);
    }

    /**
     * Delegates to the core's GUI regardless of which face of the structure got clicked - without
     * this, only the core's own single cell opens the menu, and depending on {@code FACING} that
     * cell can end up buried behind a whole row of these (solid, but otherwise inert) parts, making
     * the structure feel like it only "opens from the back".
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof HellForgePartBlockEntity partBe && partBe.getCorePos() != null
                && level.getBlockEntity(partBe.getCorePos()) instanceof HellForgeBlockEntity hellForge) {
            player.openMenu((MenuProvider) hellForge);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isCreative()) {
            CREATIVE_BREAK.add(LevelPos.of(level, pos));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean creative = CREATIVE_BREAK.remove(LevelPos.of(level, pos));
        if (!level.isClientSide && state.getBlock() != newState.getBlock() && !movedByPiston
                && level.getBlockEntity(pos) instanceof HellForgePartBlockEntity partBe && partBe.getCorePos() != null) {
            breakFromPart(level, partBe.getCorePos(), pos, creative);
        }
        super.onRemove(state, level, pos, newState, movedByPiston); // removes this part's block entity
    }

    /**
     * Removes every part around {@code corePos} (called from {@link HellForgeBlock#onRemove}).
     * {@code coreState} must be the core's OLD state (the one being removed) - by the time
     * {@code onRemove} runs, {@code level.getBlockState(corePos)} already returns the new
     * (post-removal) state, which has no {@code FACING} to compute offsets from.
     */
    public static void removeSurroundingParts(Level level, BlockPos corePos, BlockState coreState) {
        LevelPos key = LevelPos.of(level, corePos);
        TEARDOWN_IN_PROGRESS.add(key);
        try {
            for (BlockPos offset : HellForgeBlock.partOffsets(coreState)) {
                BlockPos pos = corePos.offset(offset);
                if (level.getBlockState(pos).getBlock() instanceof HellForgePartBlock) {
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            TEARDOWN_IN_PROGRESS.remove(key);
        }
    }

    /** Called from {@link #onRemove} when a part itself is the one being broken. */
    private static void breakFromPart(Level level, BlockPos corePos, BlockPos partPos, boolean creative) {
        if (TEARDOWN_IN_PROGRESS.contains(LevelPos.of(level, corePos))) {
            // Already being torn down by the core's own onRemove - that call is what's responsible
            // for the (single) drop, so don't do anything else here.
            return;
        }

        // The core is still standing at this point (this part is what's being broken, not the
        // core), so its live state is exactly the state we need - removeBlock below triggers
        // HellForgeBlock#onRemove, which cleans up every other part.
        BlockState coreState = level.getBlockState(corePos);
        if (coreState.getBlock() instanceof HellForgeBlock) {
            level.removeBlock(corePos, false);
            // Only drop when THIS call is the one actually tearing the structure down - if the core
            // was already gone (e.g. an orphaned part left over from an earlier bug, or another part
            // of the same structure already handled it a moment before), there's nothing to give the
            // player and popping a resource here would drop a Hell Forge out of thin air every time.
            // Also skip it entirely in creative, same as every normal loot-table-driven block.
            if (!creative) {
                Block.popResource(level, partPos, new ItemStack(ModBlocks.HELL_FORGE.get()));
            }
        }
    }
}
