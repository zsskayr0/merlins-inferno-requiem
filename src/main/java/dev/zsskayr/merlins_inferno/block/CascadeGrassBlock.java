package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumBiomeSource;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumBiome;

/** Grass behavior and bonemeal vegetation, with Arganite as the underlying stone. */
public class CascadeGrassBlock extends GrassBlock {
    public static final MapCodec<GrassBlock> CODEC = simpleCodec(CascadeGrassBlock::new);

    public CascadeGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GrassBlock> codec() {
        return CODEC;
    }

    private static boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState cover = level.getBlockState(above);
        if (cover.is(Blocks.SNOW) && cover.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (cover.getFluidState().getAmount() == 8) {
            return false;
        }
        return LightEngine.getLightBlockInto(level, state, pos, cover, above, Direction.UP,
                cover.getLightBlock(level, above)) < level.getMaxLightLevel();
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        for (int i = 0; i < 96; i++) {
            BlockPos target = pos.offset(random.nextInt(7) - 3, 1 + random.nextInt(3) - 1,
                    random.nextInt(7) - 3);
            if (!level.getBlockState(target.below()).is(this) || !level.getBlockState(target).isAir()) {
                continue;
            }
            if (random.nextInt(8) == 0) {
                List<ConfiguredFeature<?, ?>> flowers = level.getBiome(target).value()
                        .getGenerationSettings().getFlowerFeatures();
                if (!flowers.isEmpty() && flowers.get(0).config() instanceof RandomPatchConfiguration patch) {
                    patch.feature().value().place(level, level.getChunkSource().getGenerator(), random, target);
                }
            } else if (random.nextInt(5) == 0 && level.getBlockState(target.above()).isAir()) {
                BlockState tallGrass = ModBlocks.TALL_PINK_GRASS.get().defaultBlockState();
                if (tallGrass.canSurvive(level, target)) {
                    DoublePlantBlock.placeAt(level, tallGrass, target, 3);
                }
            } else {
                BlockState pinkGrass = ModBlocks.PINK_GRASS.get().defaultBlockState();
                if (pinkGrass.canSurvive(level, target)) {
                    level.setBlock(target, pinkGrass, 3);
                }
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 3)) return;
        if (!canStay(state, level, pos)) {
            level.setBlockAndUpdate(pos, ModBlocks.ARGANITE.get().defaultBlockState());
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) return;

        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (!level.getBlockState(target).is(ModBlocks.ARGANITE.get())
                    || !level.getBiome(target).is(ElysiumBiomeSource.key(ElysiumBiome.CASCADE_VALE))
                    || level.getFluidState(target.above()).is(FluidTags.WATER)
                    || !canStay(defaultBlockState(), level, target)) {
                continue;
            }
            level.setBlockAndUpdate(target, defaultBlockState().setValue(SNOWY,
                    level.getBlockState(target.above()).is(BlockTags.SNOW)));
        }
    }
}
