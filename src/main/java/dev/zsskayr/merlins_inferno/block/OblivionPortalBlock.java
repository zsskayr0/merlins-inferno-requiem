package dev.zsskayr.merlins_inferno.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.zsskayr.merlins_inferno.portal.OblivionDimension;
import dev.zsskayr.merlins_inferno.portal.OblivionPortalShape;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;

/**
 * The film of an Oblivion portal, behaving like a Nether portal: it lives in an obsidian frame, vanishes when the
 * frame breaks, and teleports players who stand in it. Entering carries you to the hub in Oblivion (remembering where
 * you came from); the hub's portal brings you back to that spot. Only players travel.
 */
public class OblivionPortalBlock extends Block implements Portal {
    public static final MapCodec<OblivionPortalBlock> CODEC = simpleCodec(OblivionPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public OblivionPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public MapCodec<OblivionPortalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_AABB : X_AXIS_AABB;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos,
            BlockPos facingPos) {
        Direction.Axis facingAxis = facing.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean alongPlane = axis != facingAxis && facingAxis.isHorizontal();
        return !alongPlane && !facingState.is(this) && !new OblivionPortalShape(level, currentPos, axis).isComplete()
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof Player && entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof Player player
                ? Math.max(1, level.getGameRules().getInt(player.getAbilities().invulnerable
                        ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY))
                : 0;
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        if (!(entity instanceof ServerPlayer player)) {
            return null;
        }
        if (level.dimension() != OblivionDimension.LEVEL) {
            ServerLevel oblivion = level.getServer().getLevel(OblivionDimension.LEVEL);
            if (oblivion == null) {
                return null;
            }
            player.setData(ModAttachments.OBLIVION_ORIGIN, GlobalPos.of(level.dimension(), player.blockPosition()));
            OblivionDimension.ensureHub(oblivion);
            return new DimensionTransition(oblivion, Vec3.atBottomCenterOf(OblivionDimension.ARRIVAL), Vec3.ZERO, 180.0F, 0.0F,
                    DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
        }
        GlobalPos origin = player.getExistingData(ModAttachments.OBLIVION_ORIGIN).orElse(null);
        ServerLevel back = origin == null ? level.getServer().overworld() : level.getServer().getLevel(origin.dimension());
        if (back == null) {
            back = level.getServer().overworld();
            origin = null;
        }
        Vec3 target = origin == null
                ? player.adjustSpawnLocation(back, back.getSharedSpawnPos()).getBottomCenter()
                : Vec3.atBottomCenterOf(origin.pos());
        return new DimensionTransition(back, target, Vec3.ZERO, player.getYRot(), player.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    0.5F, random.nextFloat() * 0.4F + 0.5F, false);
        }
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double vx = (random.nextFloat() - 0.5) * 0.5;
            double vy = (random.nextFloat() - 0.5) * 0.5;
            double vz = (random.nextFloat() - 0.5) * 0.5;
            int side = random.nextInt(2) * 2 - 1;
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = pos.getX() + 0.5 + 0.25 * side;
                vx = random.nextFloat() * 2.0F * side;
            } else {
                z = pos.getZ() + 0.5 + 0.25 * side;
                vz = random.nextFloat() * 2.0F * side;
            }
            level.addParticle(ParticleTypes.SQUID_INK, x, y, z, vx * 0.1, vy * 0.1, vz * 0.1);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        if (rot == Rotation.COUNTERCLOCKWISE_90 || rot == Rotation.CLOCKWISE_90) {
            return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z);
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
