package dev.zsskayr.merlins_inferno.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.zsskayr.merlins_inferno.portal.ElysiumDimension;
import dev.zsskayr.merlins_inferno.portal.ElysiumPortalShape;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModParticles;

/**
 * The film of an El'ysium portal, behaving like a Nether portal: it lives in an obsidian frame, vanishes when the
 * frame breaks, and teleports players who stand in it. Entering carries you to the portal nearest your scaled coordinates
 * in Elysium (remembering where you came from); that portal brings you back to that spot, and any other opens at its own
 * scaled coordinates outside (see {@link ElysiumDimension}). Only players travel.
 */
public class ElysiumPortalBlock extends Block implements Portal {
    public static final MapCodec<ElysiumPortalBlock> CODEC = simpleCodec(ElysiumPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** Which sides of the portal film touch the frame (drawn as a glowing rim); set once, when the portal is lit. */
    public static final BooleanProperty EDGE_LEFT = BooleanProperty.create("edge_left");
    public static final BooleanProperty EDGE_RIGHT = BooleanProperty.create("edge_right");
    public static final BooleanProperty EDGE_UP = BooleanProperty.create("edge_up");
    public static final BooleanProperty EDGE_DOWN = BooleanProperty.create("edge_down");
    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    /** Any film block of the widest possible frame is this close to the spot in front of it. */
    private static final double GATE_REACH = 32.0;

    public ElysiumPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(EDGE_LEFT, false)
                .setValue(EDGE_RIGHT, false).setValue(EDGE_UP, false).setValue(EDGE_DOWN, false));
    }

    @Override
    public MapCodec<ElysiumPortalBlock> codec() {
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
        return !alongPlane && !facingState.is(this) && !new ElysiumPortalShape(level, currentPos, axis).isComplete()
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
        if (level.dimension() != ElysiumDimension.LEVEL) {
            ServerLevel elysium = level.getServer().getLevel(ElysiumDimension.LEVEL);
            if (elysium == null) {
                return null;
            }
            // El'ysium is the Angelical route's third Circle: the way in stays shut until the world has reached it.
            if (!dev.zsskayr.merlins_inferno.attachment.ProgressionHelper.hasReached(player, ElysiumDimension.REQUIRED_CIRCLE)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.merlins_inferno.elysium_locked"), true);
                return null;
            }
            player.setData(ModAttachments.ELYSIUM_ORIGIN, GlobalPos.of(level.dimension(), player.blockPosition()));
            BlockPos arrival = ElysiumDimension.ensureArrival(elysium, player.blockPosition());
            player.setData(ModAttachments.ELYSIUM_GATE, arrival);
            return new DimensionTransition(elysium, Vec3.atBottomCenterOf(arrival), Vec3.ZERO, 180.0F, 0.0F,
                    DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
        }
        GlobalPos origin = player.getExistingData(ModAttachments.ELYSIUM_ORIGIN).orElse(null);
        BlockPos gate = player.getExistingData(ModAttachments.ELYSIUM_GATE).orElse(null);
        if (origin == null || (gate != null && !gate.closerThan(pos, GATE_REACH))) {
            ServerLevel overworld = level.getServer().overworld();
            return new DimensionTransition(overworld, Vec3.atBottomCenterOf(ElysiumDimension.ensureExit(overworld, pos)), Vec3.ZERO, player.getYRot(), player.getXRot(),
                    DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
        }
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
        // Sanct motes rise off both faces of the film and drift away, as they do from raw Lyrium.
        boolean alongX = state.getValue(AXIS) == Direction.Axis.X;
        for (int i = 0; i < 3; i++) {
            double across = 0.5 + (random.nextInt(2) * 2 - 1) * 0.15;
            double x = pos.getX() + (alongX ? random.nextDouble() : across);
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + (alongX ? across : random.nextDouble());
            level.addParticle(ModParticles.SANCTIFIED_TYPE, x, y, z, 0.0, 0.0, 0.0);
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
        builder.add(AXIS, EDGE_LEFT, EDGE_RIGHT, EDGE_UP, EDGE_DOWN);
    }
}
