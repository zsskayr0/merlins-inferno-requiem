package dev.zsskayr.merlins_inferno.portal;

import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.block.OblivionPortalBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * The shape of an Oblivion portal: the Nether portal's rules exactly (a Void Block rectangle, 2-21 wide and 3-21 tall
 * inside), filled with Oblivion portal blocks instead. A trimmed copy of vanilla's {@code PortalShape}, which is
 * hard-wired to the Nether portal block.
 */
public class OblivionPortalShape {
    private static final int MIN_WIDTH = 2;
    private static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 21;

    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final Direction rightDir;
    private int numPortalBlocks;
    @Nullable
    private BlockPos bottomLeft;
    private int height;
    private final int width;

    public static Optional<OblivionPortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos bottomLeft, Direction.Axis axis) {
        return findPortalShape(level, bottomLeft, shape -> shape.isValid() && shape.numPortalBlocks == 0, axis);
    }

    public static Optional<OblivionPortalShape> findPortalShape(LevelAccessor level, BlockPos bottomLeft, Predicate<OblivionPortalShape> predicate,
            Direction.Axis axis) {
        Optional<OblivionPortalShape> shape = Optional.of(new OblivionPortalShape(level, bottomLeft, axis)).filter(predicate);
        if (shape.isPresent()) {
            return shape;
        }
        Direction.Axis other = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return Optional.of(new OblivionPortalShape(level, bottomLeft, other)).filter(predicate);
    }

    public OblivionPortalShape(LevelAccessor level, BlockPos bottomLeft, Direction.Axis axis) {
        this.level = level;
        this.axis = axis;
        this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        this.bottomLeft = this.calculateBottomLeft(bottomLeft);
        if (this.bottomLeft == null) {
            this.bottomLeft = bottomLeft;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = this.calculateWidth();
            if (this.width > 0) {
                this.height = this.calculateHeight();
            }
        }
    }

    @Nullable
    private BlockPos calculateBottomLeft(BlockPos pos) {
        int minY = Math.max(this.level.getMinBuildHeight(), pos.getY() - MAX_HEIGHT);
        while (pos.getY() > minY && isEmpty(this.level.getBlockState(pos.below()))) {
            pos = pos.below();
        }
        Direction left = this.rightDir.getOpposite();
        int distance = this.getDistanceUntilEdgeAboveFrame(pos, left) - 1;
        return distance < 0 ? null : pos.relative(left, distance);
    }

    private int calculateWidth() {
        int distance = this.getDistanceUntilEdgeAboveFrame(this.bottomLeft, this.rightDir);
        return distance >= MIN_WIDTH && distance <= MAX_WIDTH ? distance : 0;
    }

    private int getDistanceUntilEdgeAboveFrame(BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i <= MAX_WIDTH; i++) {
            cursor.set(pos).move(direction, i);
            BlockState state = this.level.getBlockState(cursor);
            if (!isEmpty(state)) {
                if (isFrame(state)) {
                    return i;
                }
                break;
            }
            BlockState below = this.level.getBlockState(cursor.move(Direction.DOWN));
            if (!isFrame(below)) {
                break;
            }
        }
        return 0;
    }

    private int calculateHeight() {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int distance = this.getDistanceUntilTop(cursor);
        return distance >= MIN_HEIGHT && distance <= MAX_HEIGHT && this.hasTopFrame(cursor, distance) ? distance : 0;
    }

    private boolean hasTopFrame(BlockPos.MutableBlockPos cursor, int distanceToTop) {
        for (int i = 0; i < this.width; i++) {
            cursor.set(this.bottomLeft).move(Direction.UP, distanceToTop).move(this.rightDir, i);
            if (!isFrame(this.level.getBlockState(cursor))) {
                return false;
            }
        }
        return true;
    }

    private int getDistanceUntilTop(BlockPos.MutableBlockPos cursor) {
        for (int i = 0; i < MAX_HEIGHT; i++) {
            cursor.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, -1);
            if (!isFrame(this.level.getBlockState(cursor))) {
                return i;
            }
            cursor.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, this.width);
            if (!isFrame(this.level.getBlockState(cursor))) {
                return i;
            }
            for (int j = 0; j < this.width; j++) {
                cursor.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, j);
                BlockState state = this.level.getBlockState(cursor);
                if (!isEmpty(state)) {
                    return i;
                }
                if (state.is(ModBlocks.OBLIVION_PORTAL.get())) {
                    this.numPortalBlocks++;
                }
            }
        }
        return MAX_HEIGHT;
    }

    private static boolean isFrame(BlockState state) {
        return state.is(ModBlocks.VOID_BLOCK.get());
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(ModBlocks.OBLIVION_PORTAL.get());
    }

    public boolean isValid() {
        return this.bottomLeft != null && this.width >= MIN_WIDTH && this.width <= MAX_WIDTH && this.height >= MIN_HEIGHT && this.height <= MAX_HEIGHT;
    }

    public void createPortalBlocks() {
        BlockState portal = ModBlocks.OBLIVION_PORTAL.get().defaultBlockState().setValue(OblivionPortalBlock.AXIS, this.axis);
        for (int w = 0; w < this.width; w++) {
            for (int h = 0; h < this.height; h++) {
                BlockPos pos = this.bottomLeft.relative(Direction.UP, h).relative(this.rightDir, w);
                this.level.setBlock(pos, portal.setValue(OblivionPortalBlock.EDGE_LEFT, w == 0).setValue(OblivionPortalBlock.EDGE_RIGHT, w == this.width - 1)
                        .setValue(OblivionPortalBlock.EDGE_DOWN, h == 0).setValue(OblivionPortalBlock.EDGE_UP, h == this.height - 1), 18);
            }
        }
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }
}
