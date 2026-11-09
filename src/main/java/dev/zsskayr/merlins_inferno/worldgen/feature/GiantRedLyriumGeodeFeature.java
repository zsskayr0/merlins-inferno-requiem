package dev.zsskayr.merlins_inferno.worldgen.feature;

import com.mojang.serialization.Codec;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A 31-block C3 geode with a corrupted obsidian shell, buried like El'ysium's landmark. */
public class GiantRedLyriumGeodeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int RADIUS = 15;
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/giant_red_lyrium_geode"));

    public GiantRedLyriumGeodeFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (surface < level.getMinBuildHeight() + 26 || surface + 11 >= level.getMaxBuildHeight()) return false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int nearby = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,
                    origin.getX() + side.getStepX() * 12, origin.getZ() + side.getStepZ() * 12);
            if (Math.abs(nearby - surface) > 6) return false;
        }
        BlockPos center = new BlockPos(origin.getX(), surface - 5, origin.getZ());
        if (level.getBlockState(center.below(18)).isAir()) return false;
        for (int x = -RADIUS; x <= RADIUS; x++) for (int y = -RADIUS; y <= RADIUS; y++) for (int z = -RADIUS; z <= RADIUS; z++) {
            double distance = Math.sqrt(x * x + y * y + z * z) + random.nextDouble() * 0.6;
            if (distance > RADIUS) continue;
            BlockPos at = center.offset(x, y, z);
            if (distance >= 12.5) level.setBlock(at, ModBlocks.CORRUPTED_OBSIDIAN.get().defaultBlockState(), 4);
            else if (distance >= 10.5) level.setBlock(at, ModBlocks.BLIGHTSTONE.get().defaultBlockState(), 4);
            else if (distance >= 8.5) level.setBlock(at, ModBlocks.RED_LYRIUM_BLOCK.get().defaultBlockState(), 4);
            else level.setBlock(at, Blocks.AIR.defaultBlockState(), 4);
        }
        for (int x = -9; x <= 9; x++) for (int y = -9; y <= 9; y++) for (int z = -9; z <= 9; z++) {
            BlockPos at = center.offset(x, y, z);
            if (!level.getBlockState(at).is(ModBlocks.RED_LYRIUM_BLOCK.get())) continue;
            for (Direction side : Direction.values()) {
                BlockPos tip = at.relative(side);
                if (level.getBlockState(tip).isAir() && random.nextFloat() < 0.18F) {
                    level.setBlock(tip, ModBlocks.RED_LYRIUM_CLUSTER.get().defaultBlockState()
                            .setValue(AmethystClusterBlock.FACING, side), 4);
                }
            }
        }
        // The prize at the bottom of the hollow (Lootr picks it up as a per-player chest when installed).
        BlockPos chest = center.below(7);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
        return true;
    }
}
