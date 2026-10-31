package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

/**
 * One column of El'ysium, filled by {@link ElysiumWorld#sample}. Reusable: sample() overwrites every field.
 *
 * Layout from bottom to top (all inclusive, absolute Y):
 *   hang     : hangLength blocks of {@link #hangMat} directly below {@code bottom} (underside dressing), 0 = none
 *   solid    : bottom..top, block at y = {@link ElysiumWorld#material(Column, int)}
 *   water    : top+1 .. waterTop when waterTop > top (WATER, or ICE on the top block if frozen)
 *   arch     : archBottom..archTop, a second solid span floating above (natural arches / Chainfall bridges), when archTop >= archBottom
 *
 * Invariants the validator enforces (see docs/elysium/ARCHITECTURE.md):
 *   - land == false  => nothing at all in this column (except arch, never water)
 *   - every water block is held: each horizontal neighbour at the same y is solid, water, or listed as a spill cell
 *   - no gravity block (SAND, GRAVEL, SNOW? no: SAND/GRAVEL/CONCRETE_POWDER) with air directly below it
 */
public final class Column {
    public int x, z;
    public boolean land;
    public int top = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
    public int waterTop = Integer.MIN_VALUE;
    public boolean frozen;
    /** True when this column's water is allowed to spill sideways (a waterfall lip at an island edge). */
    public boolean spill;
    public int archBottom = 0, archTop = -1;
    public Mat hangMat = Mat.AIR;
    public int hangLength;
    public ElysiumBiome biome = ElysiumBiome.LARK_MEADOWS;
    /** Index into ElysiumWorld island list, -1 over open sky. */
    public int island = -1;
    /** Blocks from the island's coast (0 at the edge, grows inland). */
    public double inland;
    /** Free per-column data the core uses to pick materials (surface depth, slope, river distance...). */
    public double slope, riverDist = Double.POSITIVE_INFINITY;
    public int surfaceDepth = 4;
    /** Base height of this column's island (the cone hangs below it; material() switches to underside below baseY - 10). */
    public int baseY;
    /** Smooth low-frequency noise in [-1,1] at this column (passed to the palette as {@code patch}). */
    public double patch;
    /** True for dry banks next to water (river/lake banks within ~3 blocks, sea shores up to 2 above sea level). */
    public boolean shore;

    public boolean hasWater() { return waterTop > top; }
    public boolean hasArch() { return archTop >= archBottom; }
}
