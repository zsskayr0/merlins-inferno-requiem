package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * The sacred white fire. It burns forever on blocks of the dimension's infiniburn tag (Arganite is in the Overworld's) and
 * dies out like any fire elsewhere. It never spreads and never eats blocks. Players and ordinary creatures take a plain
 * fire's damage; the unholy (see {@link LyriumRadiance#isUnholy}) take {@link #UNHOLY_DAMAGE} - twice a Soul Fire's -
 * from a damage type that is not fire, so neither fire immunity nor Fire Resistance helps. Dropped items are left alone.
 */
public class GraceFireBlock extends BaseFireBlock {
    public static final MapCodec<GraceFireBlock> CODEC = simpleCodec(GraceFireBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "grace_fire"));
    /** Soul Fire deals 2; this is twice that. */
    public static final float UNHOLY_DAMAGE = 4.0F;
    private static final float COMMON_FIRE_DAMAGE = 1.0F;

    public GraceFireBlock(Properties properties) {
        super(properties, COMMON_FIRE_DAMAGE);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends BaseFireBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return this.canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, tickDelay(level.random));
    }

    private static int tickDelay(RandomSource random) {
        return 30 + random.nextInt(10);
    }

    private static boolean isInfinite(Level level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(level.dimensionType().infiniburn());
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, tickDelay(random));
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            return;
        }
        if (!state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        if (isInfinite(level, pos)) {
            return;
        }
        int age = state.getValue(AGE);
        if (level.isRainingAt(pos) && random.nextFloat() < 0.2F + age * 0.03F) {
            level.removeBlock(pos, false);
            return;
        }
        int newAge = Math.min(15, age + random.nextInt(3) / 2);
        if (newAge == 15 && random.nextInt(4) == 0) {
            level.removeBlock(pos, false);
        } else if (newAge != age) {
            level.setBlock(pos, state.setValue(AGE, newAge), Block.UPDATE_INVISIBLE);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity) {
            return; // dropped items never burn in it
        }
        if (entity instanceof LivingEntity living && LyriumRadiance.isUnholy(living)) {
            if (!living.fireImmune()) {
                living.setRemainingFireTicks(living.getRemainingFireTicks() + 1);
                if (living.getRemainingFireTicks() == 0) {
                    living.igniteForSeconds(8.0F);
                }
            }
            if (!level.isClientSide) {
                living.hurt(damageSource(level), UNHOLY_DAMAGE);
            }
            return;
        }
        super.entityInside(state, level, pos, entity);
    }

    public static DamageSource damageSource(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE));
    }
}
