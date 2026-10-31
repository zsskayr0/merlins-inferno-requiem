package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * The block palette of El'ysium's built things, in one place so the look can be tuned without touching geometry.
 * Loaded lazily on the first structure build (after registries are frozen), so reading {@code ModBlocks} here is safe.
 */
final class Pal {
    private Pal() {}

    // ---- dark stone (bridges, ruins) ----
    static final BlockState ARG = ModBlocks.ARGANITE.get().defaultBlockState();
    static final BlockState ARG_BRICKS = ModBlocks.ARGANITE_BRICKS.get().defaultBlockState();
    static final BlockState ARG_BRICK_SLAB = ModBlocks.ARGANITE_BRICKS_SLAB.get().defaultBlockState();
    static final BlockState ARG_BRICK_STAIRS = ModBlocks.ARGANITE_BRICKS_STAIRS.get().defaultBlockState();
    static final BlockState ARG_BRICK_WALL = ModBlocks.ARGANITE_BRICKS_WALL.get().defaultBlockState();
    static final BlockState ARG_SLAB = ModBlocks.ARGANITE_SLAB.get().defaultBlockState();
    static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
    static final BlockState MOSSY_BRICKS = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    static final BlockState CRACKED_BRICKS = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    static final BlockState CHISELED_BRICKS = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    static final BlockState MOSSY_BRICK_SLAB = Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState();
    static final BlockState MOSSY_BRICK_STAIRS = Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState();
    static final BlockState STONE_BRICK_SLAB = Blocks.STONE_BRICK_SLAB.defaultBlockState();
    static final BlockState STONE_BRICK_STAIRS = Blocks.STONE_BRICK_STAIRS.defaultBlockState();
    static final BlockState MOSSY_BRICK_WALL = Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState();
    static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
    static final BlockState MOSSY_COBBLE = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    static final BlockState MOSSY_COBBLE_SLAB = Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState();
    static final BlockState COBBLE_SLAB = Blocks.COBBLESTONE_SLAB.defaultBlockState();
    static final BlockState STONE = Blocks.STONE.defaultBlockState();
    static final BlockState SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
    static final BlockState TUFF_BRICKS = Blocks.TUFF_BRICKS.defaultBlockState();
    static final BlockState POLISHED_ANDESITE = Blocks.POLISHED_ANDESITE.defaultBlockState();

    // ---- dark metal (giant chains) ----
    static final BlockState BLACKSTONE = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    static final BlockState BLACKSTONE_BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();

    // ---- rusted metal (industrial ruin) ----
    static final BlockState OX_COPPER = Blocks.OXIDIZED_COPPER.defaultBlockState();
    static final BlockState WE_COPPER = Blocks.WEATHERED_COPPER.defaultBlockState();
    static final BlockState OX_CUT = Blocks.OXIDIZED_CUT_COPPER.defaultBlockState();
    static final BlockState WE_CUT = Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
    static final BlockState OX_CUT_SLAB = Blocks.OXIDIZED_CUT_COPPER_SLAB.defaultBlockState();
    static final BlockState WE_CUT_SLAB = Blocks.WEATHERED_CUT_COPPER_SLAB.defaultBlockState();
    static final BlockState OX_GRATE = Blocks.OXIDIZED_COPPER_GRATE.defaultBlockState();
    static final BlockState IRON_BARS = Blocks.IRON_BARS.defaultBlockState();
    static final BlockState IRON_BLOCK = Blocks.IRON_BLOCK.defaultBlockState();
    static final BlockState RAIL = Blocks.RAIL.defaultBlockState();

    // ---- wood & rope ----
    static final BlockState SPRUCE_PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
    static final BlockState SPRUCE_SLAB = Blocks.SPRUCE_SLAB.defaultBlockState();
    static final BlockState OAK_SLAB = Blocks.OAK_SLAB.defaultBlockState();
    static final BlockState SPRUCE_FENCE = Blocks.SPRUCE_FENCE.defaultBlockState();
    static final BlockState SPRUCE_LOG = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();

    // ---- white architecture ----
    static final BlockState PORCELAIN = ModBlocks.PORCELAIN.get().defaultBlockState();
    static final BlockState PORCELAIN_SLAB = ModBlocks.PORCELAIN_SLAB.get().defaultBlockState();
    static final BlockState PORCELAIN_STAIRS = ModBlocks.PORCELAIN_STAIRS.get().defaultBlockState();
    static final BlockState POL_PORCELAIN = ModBlocks.POLISHED_PORCELAIN.get().defaultBlockState();
    static final BlockState POL_PORCELAIN_SLAB = ModBlocks.POLISHED_PORCELAIN_SLAB.get().defaultBlockState();
    static final BlockState POL_PORCELAIN_STAIRS = ModBlocks.POLISHED_PORCELAIN_STAIRS.get().defaultBlockState();
    static final BlockState TILED_PORCELAIN = ModBlocks.TILED_PORCELAIN.get().defaultBlockState();
    static final BlockState QUARTZITE = ModBlocks.QUARTZITE.get().defaultBlockState();
    static final BlockState QUARTZ_CLUSTER = ModBlocks.QUARTZ_CLUSTER.get().defaultBlockState();
    static final BlockState LYRIUM_CLUSTER = ModBlocks.LYRIUM_CLUSTER.get().defaultBlockState();
    static final BlockState QUARTZ_PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    static final BlockState QUARTZ_BLOCK = Blocks.QUARTZ_BLOCK.defaultBlockState();
    static final BlockState QUARTZ_BRICKS = Blocks.QUARTZ_BRICKS.defaultBlockState();
    static final BlockState CHISELED_QUARTZ = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
    static final BlockState SMOOTH_QUARTZ = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    static final BlockState SMOOTH_QUARTZ_SLAB = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
    static final BlockState QUARTZ_STAIRS = Blocks.QUARTZ_STAIRS.defaultBlockState();
    static final BlockState QUARTZ_SLAB = Blocks.QUARTZ_SLAB.defaultBlockState();
    static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();
    static final BlockState WHITE_GLASS = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
    static final BlockState WHITE_PANE = Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState();
    static final BlockState SEA_LANTERN = Blocks.SEA_LANTERN.defaultBlockState();
    static final BlockState END_ROD_UP = Blocks.END_ROD.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);

    // ---- light, chain, greenery ----
    static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    static final BlockState LANTERN_HANGING = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    static final BlockState CHAIN_Y = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
    static final BlockState MOSS_BLOCK = Blocks.MOSS_BLOCK.defaultBlockState();
    static final BlockState MOSS_CARPET = Blocks.MOSS_CARPET.defaultBlockState();
    static final BlockState VINE = Blocks.VINE.defaultBlockState();
    static final BlockState AZALEA_LEAVES = Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    static final BlockState FLOWERING_AZALEA_LEAVES = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    static final BlockState CHEST = Blocks.CHEST.defaultBlockState();
    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** A chain block lying along a horizontal axis. */
    static BlockState chain(Direction.Axis axis) {
        return Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, axis);
    }

    /** A pillar block (log, quartz pillar...) along the axis. */
    static BlockState pillar(BlockState state, Direction.Axis axis) {
        return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
    }

    /** A slab of the given type (BOTTOM / TOP / DOUBLE). Non-slabs are returned unchanged. */
    static BlockState slab(BlockState slab, SlabType type) {
        return slab.hasProperty(SlabBlock.TYPE) ? slab.setValue(SlabBlock.TYPE, type) : slab;
    }

    /**
     * Stairs whose high side faces {@code facing} (you walk up them going towards {@code facing}); {@code upsideDown}
     * flips them under a ceiling. Non-stairs are returned unchanged.
     */
    static BlockState stairs(BlockState stairs, Direction facing, boolean upsideDown) {
        if (!stairs.hasProperty(StairBlock.FACING)) {
            return stairs;
        }
        return stairs.setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
    }

    /** Weighted ruin masonry: mostly stone bricks, mossy and cracked mixed in, a little Arganite. {@code r} in [0,1). */
    static BlockState ruinBrick(double r) {
        if (r < 0.40) return STONE_BRICKS;
        if (r < 0.70) return MOSSY_BRICKS;
        if (r < 0.82) return CRACKED_BRICKS;
        if (r < 0.92) return ARG_BRICKS;
        return MOSSY_COBBLE;
    }

    /** Weighted rusted metal. {@code r} in [0,1). */
    static BlockState rust(double r) {
        if (r < 0.45) return OX_CUT;
        if (r < 0.70) return WE_CUT;
        if (r < 0.85) return OX_COPPER;
        return WE_COPPER;
    }

    static boolean is(BlockState state, Block block) {
        return state.is(block);
    }
}
