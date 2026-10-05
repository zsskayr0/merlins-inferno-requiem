package dev.zsskayr.merlins_inferno.block;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.SacredAltarPartBlockEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * The altar of a Sacred Church. A diamond laid on it - a tithe - keeps the vigil again: the altar plays its activation
 * (the prongs lower and spin around the crystal), then rehangs the Great Bell and wakes Elias, once the Sacred Priest that guards the church has been
 * defeated, the day-long cooldown after the last Elias's death has passed and no Elias is already awake. Unbreakable in survival (it holds the shrine's state).
 * <p>
 * The model is far bigger than one block, so the altar carries a 3 x 3 x 2 hitbox of invisible {@link SacredAltarPartBlock}s,
 * built the way the Hell Forge's is (see {@link HellForgeBlock}).
 */
public class SacredAltarBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SacredAltarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Like a furnace: the altar faces the player who places it. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!footprintFree(context)) {
            return null;
        }
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

    /** Ticks the altar on the server: it tends the church's congregation (see {@link SacredAltarBlockEntity#serverTick}). */
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntityTypes.SACRED_ALTAR.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<SacredAltarBlockEntity>) (l, pos, st, altar) -> altar.serverTick((ServerLevel) l);
    }

    /** GeckoLib draws the altar (the baked cube is only the break-particle and item look). */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SacredAltarBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        return useOffering(stack, level, pos, player);
    }

    /** The diamond laid on the altar at {@code pos}; shared with {@link SacredAltarPartBlock}, which forwards its clicks here. */
    static ItemInteractionResult useOffering(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!stack.is(Items.DIAMOND)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof SacredAltarBlockEntity altar)) {
            return ItemInteractionResult.CONSUME;
        }
        if (altar.isPriestPending() && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.sacred_altar.guarded"), true);
        } else if (altar.isActivating()) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.sacred_altar.stirring"), true);
        } else if (altar.hasLivingElias(serverLevel)) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.sacred_altar.awake"), true);
        } else if (!altar.isReady(level)) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.sacred_altar.cooldown", altar.remainingMinutes(level)), true);
        } else if (altar.invoke(serverLevel)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.merlins_inferno.sacred_altar.invoked"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.sacred_altar.stirring"), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return showHint(level, player);
    }

    static InteractionResult showHint(Level level, Player player) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.sacred_altar.hint"), true);
        }
        return InteractionResult.SUCCESS;
    }

    // --- the 3 x 3 x 2 hitbox: invisible parts around the altar, see SacredAltarPartBlock ---

    /** The 17 cells (relative to the altar) that get a part: the 3 x 3 footprint, two high, minus the altar's own cell. */
    public static final List<BlockPos> PART_OFFSETS = partOffsets();

    private static List<BlockPos> partOffsets() {
        List<BlockPos> offsets = new ArrayList<>(17);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (dx != 0 || dz != 0 || dy != 0) {
                        offsets.add(new BlockPos(dx, dy, dz));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    /**
     * Fills the cells around the altar that are free with parts (and tells each where its altar is). Run when the altar is
     * placed by hand, by the church when it generates, and again by the altar itself when it loads (for altars that
     * predate the parts, or whose neighbours were not there when it generated). Only free cells are taken: the altar never
     * deletes something somebody built next to it.
     */
    public static void placeParts(LevelAccessor level, BlockPos corePos) {
        for (BlockPos offset : PART_OFFSETS) {
            BlockPos partPos = corePos.offset(offset);
            BlockState existing = level.getBlockState(partPos);
            if (existing.getBlock() instanceof SacredAltarPartBlock) {
                bind(level, partPos, corePos);
            } else if (existing.canBeReplaced() && partPos.getY() < level.getMaxBuildHeight()) {
                level.setBlock(partPos, ModBlocks.SACRED_ALTAR_PART.get().defaultBlockState(), 3);
                bind(level, partPos, corePos);
            }
        }
    }

    private static void bind(LevelAccessor level, BlockPos partPos, BlockPos corePos) {
        if (level.getBlockEntity(partPos) instanceof SacredAltarPartBlockEntity part && !corePos.equals(part.getCorePos())) {
            part.setCorePos(corePos);
        }
    }

    /** Refuses the placement if a cell of the footprint is taken (or above the build height), rather than leaving a hole in the hitbox. */
    private static boolean footprintFree(BlockPlaceContext context) {
        Level level = context.getLevel();
        for (BlockPos offset : PART_OFFSETS) {
            BlockPos partPos = context.getClickedPos().offset(offset);
            if (partPos.getY() >= level.getMaxBuildHeight() || !level.getBlockState(partPos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            placeParts(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && state.getBlock() != newState.getBlock() && !movedByPiston) {
            SacredAltarPartBlock.removeSurroundingParts(level, pos);
        }
    }
}
