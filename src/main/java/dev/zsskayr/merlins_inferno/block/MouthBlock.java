package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Flesh with a maw on top. A creature that steps on it makes it open: the floor drops half a block and the mob falls
 * in to be chewed. A mob up to 1 block tall is trapped, up to 2 blocks tall it is slowed and cannot jump well, and
 * anything bigger is only lowered and hurt. The mouth shuts again once nothing is left in it.
 */
public class MouthBlock extends Block {
    public static final int MAX_DEPTH = 4;
    /** 0 = shut; each step lowers the floor by 2 pixels, down to half a block. */
    public static final IntegerProperty DEPTH = IntegerProperty.create("depth", 0, MAX_DEPTH);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape[] COLLISION = new VoxelShape[MAX_DEPTH + 1];
    private static final int CHEW_INTERVAL = 20;
    private static final int STEP_TICKS = 1;

    static {
        for (int d = 0; d <= MAX_DEPTH; d++) {
            COLLISION[d] = Block.box(0.0, 0.0, 0.0, 16.0, 16.0 - 2.0 * d, 16.0);
        }
    }

    public MouthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DEPTH, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEPTH, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION[state.getValue(DEPTH)];
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && state.getValue(DEPTH) == 0 && entity instanceof LivingEntity) {
            level.setBlock(pos, state.setValue(DEPTH, 1), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.8F, 0.6F);
            level.scheduleTick(pos, this, STEP_TICKS);
        }
        grip(state, entity);
        super.stepOn(level, pos, state, entity);
    }

    /** Holds a mob by its size; applied from the first contact so running over the mouth does not get past it. */
    private static void grip(BlockState state, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        double height = living.getBbHeight();
        if (height <= 1.001) {
            // Swallowed whole: it cannot move at all.
            entity.makeStuckInBlock(state, new Vec3(0.01, 0.01, 0.01));
        } else if (height <= 2.001) {
            // Slowed, and a jump barely lifts it.
            entity.makeStuckInBlock(state, new Vec3(0.3, 0.5, 0.3));
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(DEPTH) == 0 || !(entity instanceof LivingEntity living)) {
            return;
        }
        double height = living.getBbHeight();
        grip(state, entity);
        if (!level.isClientSide && level.getGameTime() % CHEW_INTERVAL == 0) {
            float damage = height > 2.001 ? 2.0F : 1.0F;
            living.hurt(level.damageSources().generic(), damage);
            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.7F, 0.5F + level.random.nextFloat() * 0.2F);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int depth = state.getValue(DEPTH);
        if (depth == 0) {
            return;
        }
        AABB mouth = new AABB(pos).expandTowards(0.0, 1.0, 0.0).deflate(0.05, 0.0, 0.05);
        boolean occupied = !level.getEntitiesOfClass(LivingEntity.class, mouth, LivingEntity::isAlive).isEmpty();
        int target = occupied ? MAX_DEPTH : 0;
        if (depth != target) {
            level.setBlock(pos, state.setValue(DEPTH, depth + Integer.signum(target - depth)), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, STEP_TICKS);
        } else if (occupied) {
            level.scheduleTick(pos, this, 10);
        }
    }
}
