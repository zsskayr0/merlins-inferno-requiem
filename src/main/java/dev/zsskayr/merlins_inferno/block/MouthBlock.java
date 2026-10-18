package dev.zsskayr.merlins_inferno.block;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
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
 * It also eats flesh thrown onto it (tag {@code mouth_food}) and spits something back, piglin-barter style. After
 * {@link #FULL_AT} meals it shuts, darkens and refuses everything (mobs and items) for {@link #FULL_TICKS}.
 */
public class MouthBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final int MAX_DEPTH = 4;
    /** 0 = shut; each step lowers the floor by 2 pixels, down to half a block. */
    public static final IntegerProperty DEPTH = IntegerProperty.create("depth", 0, MAX_DEPTH);
    /** Chews left after something was fed to the mouth: it opens wide, chews, then burps the result out. */
    public static final int MAX_CHEW = 3;
    public static final IntegerProperty CHEW = IntegerProperty.create("chew", 0, MAX_CHEW);
    /** Sated: closed and darker, nothing is eaten until it wakes up again. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty FULL =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("full");
    public static final int FULL_AT = 90;
    /** Five minutes. */
    public static final long FULL_TICKS = 6000L;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape[] COLLISION = new VoxelShape[MAX_DEPTH + 1];
    private static final int CHEW_INTERVAL = 20;
    private static final int STEP_TICKS = 1;
    private static final int CHEW_STEP_TICKS = 14;
    /**
     * Ticks to wait after each opening step (indexed by the depth just left): the floor gives way like something
     * falling, slow at first and then at the speed a player drops (about half a block in four ticks).
     */
    private static final int[] OPEN_STEP_TICKS = {3, 2, 1, 1};
    /** Ticks between the steps of the mouth closing after the burp. */
    private static final int CLOSE_STEP_TICKS = 5;
    public static final TagKey<Item> MOUTH_FOOD = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "mouth_food"));
    private static final ResourceKey<LootTable> FEED_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "gameplay/mouth_block_feed"));

    static {
        for (int d = 0; d <= MAX_DEPTH; d++) {
            COLLISION[d] = Block.box(0.0, 0.0, 0.0, 16.0, 16.0 - 2.0 * d, 16.0);
        }
    }

    public MouthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DEPTH, 0).setValue(CHEW, 0).setValue(FULL, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEPTH, CHEW, FULL, FACING);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new dev.zsskayr.merlins_inferno.blockentity.MouthBlockEntity(pos, state);
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
        if (state.getValue(FULL)) {
            super.stepOn(level, pos, state, entity);
            return;
        }
        if (entity instanceof ItemEntity item && !level.isClientSide) {
            feed((ServerLevel) level, pos, item);
            return;
        }
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
        if (state.getValue(FULL)) {
            wakeWhenRested(state, level, pos);
            return;
        }
        int depth = state.getValue(DEPTH);
        int chew = state.getValue(CHEW);
        if (depth == 0 && chew == 0) {
            sateIfFed(state, level, pos);
            return;
        }
        AABB mouth = new AABB(pos).expandTowards(0.0, 1.0, 0.0).deflate(0.05, 0.0, 0.05);
        boolean occupied = !level.getEntitiesOfClass(LivingEntity.class, mouth, LivingEntity::isAlive).isEmpty();
        int target = occupied || chew > 0 ? MAX_DEPTH : 0;
        if (depth != target) {
            level.setBlock(pos, state.setValue(DEPTH, depth + Integer.signum(target - depth)), Block.UPDATE_ALL);
            // A mob in it is gripped at once; the chewing mouth opens and closes slowly.
            level.scheduleTick(pos, this, occupied ? STEP_TICKS : chew > 0 ? OPEN_STEP_TICKS[Math.min(depth, MAX_DEPTH - 1)] : CLOSE_STEP_TICKS);
        } else if (chew > 0) {
            // Wide open: chew, and on the last chew burp the result out and let the mouth close again.
            level.setBlock(pos, state.setValue(CHEW, chew - 1), Block.UPDATE_ALL);
            if (chew == 1) {
                spit(level, pos);
                level.scheduleTick(pos, this, CHEW_STEP_TICKS);
            } else {
                level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.8F, 0.45F + level.random.nextFloat() * 0.2F);
                level.scheduleTick(pos, this, CHEW_STEP_TICKS);
            }
        } else if (occupied) {
            level.scheduleTick(pos, this, 10);
        }
    }

    /** Shut and rested after the last burp: if it has eaten its fill, it closes up and darkens for five minutes. */
    private void sateIfFed(BlockState state, ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof dev.zsskayr.merlins_inferno.blockentity.MouthBlockEntity mouth && mouth.fed() >= FULL_AT) {
            mouth.sate(level.getGameTime() + FULL_TICKS);
            level.setBlock(pos, state.setValue(FULL, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.PLAYER_BURP, SoundSource.BLOCKS, 1.0F, 0.4F);
            level.scheduleTick(pos, this, (int) FULL_TICKS);
        }
    }

    private void wakeWhenRested(BlockState state, ServerLevel level, BlockPos pos) {
        dev.zsskayr.merlins_inferno.blockentity.MouthBlockEntity mouth = level.getBlockEntity(pos) instanceof dev.zsskayr.merlins_inferno.blockentity.MouthBlockEntity m ? m : null;
        long left = mouth == null ? 0 : mouth.fullUntil() - level.getGameTime();
        if (left > 0) {
            level.scheduleTick(pos, this, (int) Math.min(left, FULL_TICKS));
            return;
        }
        if (mouth != null) {
            mouth.wake();
        }
        level.setBlock(pos, state.setValue(FULL, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.8F, 0.9F);
    }

    /** Swallows one thrown item and starts chewing; the loot is rolled when the mouth burps (see tick). Busy mouths wait. */
    private static void feed(ServerLevel level, BlockPos pos, ItemEntity item) {
        ItemStack stack = item.getItem();
        BlockState state = level.getBlockState(pos);
        if (!item.isAlive() || !stack.is(MOUTH_FOOD) || !(state.getBlock() instanceof MouthBlock mouthBlock)
                || state.getValue(FULL) || state.getValue(CHEW) > 0 || state.getValue(DEPTH) > 0) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof dev.zsskayr.merlins_inferno.blockentity.MouthBlockEntity mouth) {
            mouth.addFed();
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.9F, 0.5F + level.random.nextFloat() * 0.2F);
        level.setBlock(pos, state.setValue(CHEW, MAX_CHEW), Block.UPDATE_ALL);
        level.scheduleTick(pos, mouthBlock, OPEN_STEP_TICKS[0]);
    }

    private static void spit(ServerLevel level, BlockPos pos) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(FEED_LOOT);
        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        for (ItemStack result : table.getRandomItems(params)) {
            ItemEntity spit = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, result);
            spit.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.2, 0.25, (level.random.nextDouble() - 0.5) * 0.2);
            spit.setDefaultPickUpDelay();
            level.addFreshEntity(spit);
        }
        level.playSound(null, pos, SoundEvents.PLAYER_BURP, SoundSource.BLOCKS, 0.8F, 0.7F);
    }
}
