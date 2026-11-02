package dev.zsskayr.merlins_inferno.block;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A rune-carved gate in the manner of Twilight Forest's castle doors: a full block that joins its neighbours, and a click
 * makes it (and every gate of its kind touching it, the ripple running from block to block) a ghost for a while. A ghost
 * gate can be walked through, but only by whoever its {@link Kind} lets pass; a few seconds later it turns solid again,
 * waiting while something is still standing inside it. The ripple is a scheduled tick per block, so a big wall opens like
 * a wave.
 *
 * <p>The state is {@code ACTIVE} (the block has been told to vanish, on its next tick) and {@code VANISHED} (it is a ghost).
 * Things that are not living (items, arrows, boats) pass through every ghost gate.
 */
public class GateBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty VANISHED = BooleanProperty.create("vanished");
    /** Ticks a gate stays a ghost. */
    private static final int VANISH_TICKS = 100;
    /** Ticks between one gate vanishing and its neighbours following. */
    private static final int RIPPLE_TICKS = 2;
    /** Ticks to wait before trying again to turn solid while something stands inside. */
    private static final int RETRY_TICKS = 10;

    /** Who a ghost gate lets through. The others bump into it as if it were solid. */
    public enum Kind {
        /** Everyone. */
        RUNIC(e -> true),
        /** Players only, and not while sneaking. */
        MUNDANE(e -> e instanceof Player p && !p.isShiftKeyDown()),
        /** Holds the hostile: players and passive mobs pass. */
        DEMONIC(e -> e instanceof Player || isPassive(e)),
        /** Holds the passive: players and hostile mobs pass. */
        ANGEL(e -> e instanceof Player || e instanceof Enemy),
        /** Holds the players: every creature but a player passes. */
        FAERIC(e -> !(e instanceof Player));

        private final Predicate<Entity> passes;

        Kind(Predicate<Entity> passes) {
            this.passes = passes;
        }

        public boolean lets(Entity entity) {
            // only living things are filtered; an item or an arrow has no kind
            return !(entity instanceof LivingEntity) || passes.test(entity);
        }
    }

    private static boolean isPassive(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof Enemy)
                && !(entity instanceof NeutralMob);
    }

    private final Kind kind;

    public GateBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false).setValue(VANISHED, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, VANISHED);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(VANISHED)) {
            Entity entity = context instanceof EntityCollisionContext ecc ? ecc.getEntity() : null;
            if (entity == null || kind.lets(entity)) {
                return Shapes.empty();
            }
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(VANISHED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VANISHED) ? Shapes.empty() : super.getVisualShape(state, level, pos, context);
    }

    /** Two ghosts side by side would draw the same translucent face twice. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return state.getValue(VANISHED) && adjacent.is(this) && adjacent.getValue(VANISHED) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(VANISHED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(ACTIVE) || state.getValue(VANISHED)) {
            return InteractionResult.PASS;
        }
        activate(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (!level.isClientSide && !state.getValue(ACTIVE) && !state.getValue(VANISHED) && level.hasNeighborSignal(pos)) {
            activate(level, pos, state);
        }
    }

    private void activate(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        level.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(VANISHED)) {
            if (state.getValue(ACTIVE)) {
                vanish(state, level, pos);
            }
            return;
        }
        // solid again means nobody can stand in it, whoever it let in: wait until it is empty
        if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) {
            level.scheduleTick(pos, this, RETRY_TICKS);
            return;
        }
        level.setBlockAndUpdate(pos, state.setValue(VANISHED, false).setValue(ACTIVE, false));
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.2F, 1.8F);
    }

    private void vanish(BlockState state, ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, state.setValue(VANISHED, true));
        level.scheduleTick(pos, this, VANISH_TICKS);
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.3F, 1.6F);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.4);
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            BlockState nextState = level.getBlockState(next);
            if (nextState.is(this) && !nextState.getValue(ACTIVE) && !nextState.getValue(VANISHED)) {
                level.setBlockAndUpdate(next, nextState.setValue(ACTIVE, true));
                level.scheduleTick(next, this, RIPPLE_TICKS);
            }
        }
    }
}
