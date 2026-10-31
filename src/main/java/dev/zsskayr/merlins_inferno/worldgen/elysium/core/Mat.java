package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

/**
 * Abstract block materials the pure core speaks in. The Minecraft side maps each one to a BlockState
 * (see worldgen/elysium/ElysiumBlocks). Add entries freely; never reorder or rename existing ones without updating the mapping.
 * AIR is "nothing here".
 */
public enum Mat {
    AIR,
    // stone family
    ARGANITE, QUARTZITE, CALCITE, STONE, ANDESITE, DIORITE, TUFF, SMOOTH_BASALT,
    // soils
    GRASS_BLOCK, DIRT, COARSE_DIRT, ROOTED_DIRT, PODZOL, MOSS_BLOCK, MUD, CLAY, GRAVEL, SAND, SANDSTONE,
    // cold
    SNOW_BLOCK, POWDER_SNOW, ICE, PACKED_ICE, BLUE_ICE, SNOW_LAYER,
    // fluids
    WATER,
    // underside dressing (placed below a column's bottom, hanging)
    HANGING_ROOTS, CAVE_VINES, GLOW_LICHEN_CEIL, MOSS_CARPET,
    // misc
    MOSSY_COBBLESTONE, COBBLESTONE, QUARTZ_BLOCK, AMETHYST_BLOCK
}
