package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The Edenweed shrub: it grows at the roots of the Hallowed Grove's Rowanwood trees (see
 * {@code RowanwoodTreePiece}). Like a sweet berry bush it regrows: a full-grown one is picked with a right-click
 * (1-2 Raw Edenweed Buds) and drops back to a young stage; breaking it only yields buds when it is full-grown.
 */
public class EdenweedBushBlock extends BushBlock {
    public static final MapCodec<EdenweedBushBlock> CODEC = simpleCodec(EdenweedBushBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final int PICKED_AGE = 1;
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);
    /** Neon green: the "magic trichomes". */
    private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(0.35F, 1.0F, 0.3F), 0.8F);

    public EdenweedBushBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, MAX_AGE));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE && level.getRawBrightness(pos.above(), 0) >= 9 && random.nextInt(8) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.RAW_EDENWEED_BUD.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < MAX_AGE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int count = 1 + level.random.nextInt(2);
            Block.popResource(level, pos, new ItemStack(ModItems.RAW_EDENWEED_BUD.get(), count));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            level.setBlock(pos, state.setValue(AGE, PICKED_AGE), Block.UPDATE_CLIENTS);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= MAX_AGE && random.nextInt(4) == 0) {
            level.addParticle(GLOW, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.3 + random.nextDouble() * 0.6,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.02, 0.0);
        }
    }
}
