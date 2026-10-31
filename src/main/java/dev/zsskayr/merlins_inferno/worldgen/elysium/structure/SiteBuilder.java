package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/**
 * Entry point for El'ysium's procedural structures. Called by the chunk generator once per {@link ElysiumWorld.Site}
 * whose (x, z) lies in the chunk being decorated, after vanilla biome decoration.
 *
 * <p>Site kinds (ids owned by this package; unknown ids do nothing):
 * <ul>
 *   <li>{@code overgrown_ruin} - broken colonnade ring on a cracked floor, fallen columns and lintel, moss, vines,
 *       sometimes a chest ({@link Ruins#overgrownRuin});</li>
 *   <li>{@code mossy_castle} - ruined keep corner: two crenellated walls, a tower stump, ivy and rubble
 *       ({@link Ruins#mossyCastle});</li>
 *   <li>{@code overgrown_works} - rusted copper frame, a crane with a hanging chain, rails on a bed, vines all over
 *       ({@link Ruins#overgrownWorks});</li>
 *   <li>{@code white_colonnade} - a clean curved porcelain/quartz colonnade on the coast, facing out over the clouds
 *       ({@link WhiteArchitecture#colonnade});</li>
 *   <li>{@code spire_chapel} - a small white chapel with 3-5 slender spires and a mirror floor
 *       ({@link WhiteArchitecture#spireChapel}).</li>
 * </ul>
 * Everything stays within {@link #RADIUS} of the origin and within {@link #MARGIN} blocks of the chunk (the
 * WorldGenRegion write limit); writes outside are dropped by the {@link Brush}. Builders are deterministic in the site's
 * salt.
 */
public final class SiteBuilder {
    /** How far outside the decorating chunk a site may write. */
    static final int MARGIN = 15; // 15, not 16: neighbour reads (water, vine support) at x+-1 must stay in the 3x3 region
    /** Hard radius around the site origin. */
    static final int RADIUS = 28;

    private SiteBuilder() {}

    public static void build(WorldGenLevel level, ChunkPos chunk, ElysiumWorld world, ElysiumWorld.Site site) {
        if (site == null || site.kind() == null) {
            return;
        }
        switch (site.kind()) {
            case "overgrown_ruin" -> Ruins.overgrownRuin(new SiteCtx(level, chunk, site));
            case "mossy_castle" -> Ruins.mossyCastle(new SiteCtx(level, chunk, site));
            case "overgrown_works" -> Ruins.overgrownWorks(new SiteCtx(level, chunk, site));
            case "white_colonnade" -> WhiteArchitecture.colonnade(new SiteCtx(level, chunk, site));
            case "spire_chapel" -> WhiteArchitecture.spireChapel(new SiteCtx(level, chunk, site));
            default -> {
                // unknown kind: nothing (the core may announce kinds this build does not know yet)
            }
        }
    }
}
