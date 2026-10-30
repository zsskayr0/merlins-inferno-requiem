package dev.zsskayr.merlins_inferno.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.GreatDoorBlockEntity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A double door that is one block to the player but fills a 3 x 3 opening: two leaves, each 1.5 blocks wide and 3 tall, that
 * swing open from the outer edges. It is nine cells of this same block; {@link #COL} and {@link #ROW} say which one a cell is
 * (the cell the item was used on is the bottom middle one, {@code COL 1 / ROW 0}). Every cell opens, closes and breaks together.
 * <p>
 * Seen from the front, column 0 is on the left of the door as it faces ({@link Direction#getCounterClockWise()} of
 * {@link #FACING}); the leaf sits in the back 3/16 of the cell, like a vanilla door.
 */
public class GreatDoorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty COL = IntegerProperty.create("col", 0, 2);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 2);

    private final BlockSetType type;
    /** A centred door stands in the middle of the cells' depth instead of the back 3/16, so it lines up with railings. */
    private final boolean centered;

    public GreatDoorBlock(BlockSetType type, Properties properties) {
        this(type, properties, false);
    }

    public GreatDoorBlock(BlockSetType type, Properties properties, boolean centered) {
        super(properties.sound(type.soundType()));
        this.type = type;
        this.centered = centered;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false)
                .setValue(POWERED, false).setValue(COL, 1).setValue(ROW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, COL, ROW);
    }

    // --- drawing: the bottom middle cell carries a GeckoLib block entity that draws the whole door ---

    private static boolean isOrigin(BlockState state) {
        return state.getValue(COL) == 1 && state.getValue(ROW) == 0;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return isOrigin(state) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isOrigin(state) ? new GreatDoorBlockEntity(pos, state) : null;
    }

    // --- the nine cells ---

    /** Where cell ({@code col}, {@code row}) is, given the bottom middle cell. */
    private static BlockPos cell(BlockPos origin, Direction facing, int col, int row) {
        return origin.relative(facing.getCounterClockWise(), 1 - col).above(row);
    }

    private static BlockPos originOf(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        return pos.relative(facing.getCounterClockWise(), -(1 - state.getValue(COL))).below(state.getValue(ROW));
    }

    // --- placing ---

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                BlockPos pos = cell(origin, facing, col, row);
                if (pos.getY() >= level.getMaxBuildHeight() || !level.getBlockState(pos).canBeReplaced(context)) {
                    return null;
                }
            }
        }
        boolean powered = level.hasNeighborSignal(origin);
        return this.defaultBlockState().setValue(FACING, facing).setValue(POWERED, powered).setValue(OPEN, powered)
                .setValue(COL, 1).setValue(ROW, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                if (col != 1 || row != 0) {
                    level.setBlock(cell(pos, facing, col, row), state.setValue(COL, col).setValue(ROW, row), 3);
                }
            }
        }
    }

    /** A cell whose neighbour in the door is gone goes too, so a broken or blown-up door never leaves leaves hanging. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        int col = state.getValue(COL);
        int row = state.getValue(ROW);
        Direction left = state.getValue(FACING).getCounterClockWise();
        int expectedCol = col;
        int expectedRow = row;
        if (direction == Direction.UP) {
            expectedRow = row + 1;
        } else if (direction == Direction.DOWN) {
            expectedRow = row - 1;
        } else if (direction == left) {
            expectedCol = col - 1;
        } else if (direction == left.getOpposite()) {
            expectedCol = col + 1;
        } else {
            return state;
        }
        if (expectedCol < 0 || expectedCol > 2 || expectedRow < 0 || expectedRow > 2) {
            return state;
        }
        boolean intact = neighborState.is(this) && neighborState.getValue(FACING) == state.getValue(FACING)
                && neighborState.getValue(COL) == expectedCol && neighborState.getValue(ROW) == expectedRow;
        return intact ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    /** One item for the whole door: the loot table only gives it from the bottom middle cell, so the other cells hand it over here. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            boolean origin = state.getValue(COL) == 1 && state.getValue(ROW) == 0;
            if (!origin && !player.isCreative()) {
                Block.popResource(level, pos, new ItemStack(this));
            }
            BlockPos base = originOf(pos, state);
            Direction facing = state.getValue(FACING);
            for (int col = 0; col < 3; col++) {
                for (int row = 0; row < 3; row++) {
                    BlockPos other = cell(base, facing, col, row);
                    if (!other.equals(pos) && level.getBlockState(other).is(this)) {
                        // No shape updates (UPDATE_KNOWN_SHAPE): they would make the cells left standing remove themselves with a drop.
                        level.setBlock(other, Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // --- opening ---

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!this.type.canOpenByHand()) {
            return InteractionResult.PASS;
        }
        setOpen(player, level, state, pos, !state.getValue(OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void setOpen(@Nullable net.minecraft.world.entity.Entity actor, Level level, BlockState state, BlockPos pos, boolean open) {
        if (state.getValue(OPEN) == open) {
            return;
        }
        BlockPos base = originOf(pos, state);
        Direction facing = state.getValue(FACING);
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                BlockPos other = cell(base, facing, col, row);
                BlockState otherState = level.getBlockState(other);
                if (otherState.is(this)) {
                    level.setBlock(other, otherState.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
                }
            }
        }
        level.playSound(actor instanceof Player p ? p : null, base, open ? this.type.doorOpen() : this.type.doorClose(),
                SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(actor, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, base);
    }

    /** Redstone next to any of the nine cells works the door, the way it does a vanilla one. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide || block == this) {
            return;
        }
        BlockPos base = originOf(pos, state);
        Direction facing = state.getValue(FACING);
        boolean powered = false;
        for (int col = 0; col < 3 && !powered; col++) {
            for (int row = 0; row < 3 && !powered; row++) {
                powered = level.hasNeighborSignal(cell(base, facing, col, row));
            }
        }
        if (powered == state.getValue(POWERED)) {
            return;
        }
        setOpen(null, level, state, pos, powered);
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                BlockPos other = cell(base, facing, col, row);
                BlockState otherState = level.getBlockState(other);
                if (otherState.is(this)) {
                    level.setBlock(other, otherState.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // --- shape ---

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    /**
     * Closed, every cell is a 3/16-thick slab at the back. Open, the outer columns keep a slab along their outer edge (the leaf
     * stands out to the front, 1.5 blocks long, but only the part inside the cell collides) and the middle column is empty.
     */
    private VoxelShape shapeOf(BlockState state) {
        boolean open = state.getValue(OPEN);
        int col = state.getValue(COL);
        double x1 = centered ? 6.5 : 0, x2 = centered ? 9.5 : 3, z1 = 0, z2 = 16;
        if (open) {
            if (col == 1) {
                return Shapes.empty();
            }
            x2 = 16;
            z1 = col == 0 ? 0 : 13;
            z2 = col == 0 ? 3 : 16;
        }
        // The box is built for FACING = east; each step to south, west, north turns it a quarter clockwise around the cell.
        int turns = switch (state.getValue(FACING)) {
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 3;
            default -> 0;
        };
        double ax = x1, az = z1, bx = x2, bz = z2;
        for (int i = 0; i < turns; i++) {
            double nax = 16 - az, nbx = 16 - bz;
            double naz = ax, nbz = bx;
            ax = nax;
            bx = nbx;
            az = naz;
            bz = nbz;
        }
        return Block.box(Math.min(ax, bx), 0, Math.min(az, bz), Math.max(ax, bx), 16, Math.max(az, bz));
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
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType pathComputationType) {
        return switch (pathComputationType) {
            case LAND, AIR -> state.getValue(OPEN);
            default -> false;
        };
    }
}
