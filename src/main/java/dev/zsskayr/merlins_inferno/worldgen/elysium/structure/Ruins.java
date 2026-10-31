package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * The dark ruins taken by nature: a broken colonnade, a mossy castle corner and an overgrown industrial works.
 * Built in the site frame ({@link SiteCtx}); every loose piece (fallen columns, rubble, rails) rests on the ground of
 * its own column, so nothing floats.
 */
final class Ruins {
    private Ruins() {}

    /** Outer radius of the castle's tower stump. */
    private static final double TOWER_RADIUS = 3.6;

    // ============================================================================================ overgrown_ruin

    /**
     * A ring of 7-10 columns (some whole with capitals and lintels, most snapped off) on a cracked, partly missing
     * floor; fallen drums lying in the grass, a fallen lintel, moss and vines; a chest in the middle half the time.
     */
    static void overgrownRuin(SiteCtx c) {
        Rng rng = c.rng;
        int f = c.floorY;
        int radius = rng.range(6, 9);
        // floor: level with the origin's ground, cut into rises and propped over dips (at most 4 deep / 3 high)
        for (int u = -radius; u <= radius; u++) {
            for (int v = -radius; v <= radius; v++) {
                double dist = Math.hypot(u, v);
                if (dist > radius + 0.5) {
                    continue;
                }
                double keep = 1.0 - 0.65 * (dist / radius) * (dist / radius);
                if (c.noise(u, 0, v) > keep) {
                    continue; // missing slab: grass shows through
                }
                int g = c.ground(u, v);
                if (g == Brush.NONE || g > f + 3 || g < f - 4) {
                    continue;
                }
                if (g > f) {
                    c.clearRange(u, v, f + 1, g);
                }
                c.plinth(u, v, f, Pal.COBBLE, 4);
                double r = c.noise(u, 1, v);
                c.set(u, f, v, r < 0.35 ? Pal.CRACKED_BRICKS : Pal.ruinBrick(r));
                c.clearPlants(u, v, f + 1, f + 2);
                if (c.noise(u, 2, v) < 0.22) {
                    c.carpet(u, f + 1, v, Pal.MOSS_CARPET);
                }
            }
        }
        // columns
        int n = rng.range(7, 10);
        double ring = radius - 1.5;
        double phase = rng.nextDouble() * Math.PI * 2.0;
        int[] cu = new int[n], cv = new int[n], ch = new int[n];
        boolean[] whole = new boolean[n];
        for (int i = 0; i < n; i++) {
            double a = phase + i * Math.PI * 2.0 / n;
            cu[i] = (int) Math.round(Math.cos(a) * ring);
            cv[i] = (int) Math.round(Math.sin(a) * ring);
            whole[i] = rng.chance(0.35);
            ch[i] = whole[i] ? rng.range(6, 7) : rng.range(1, 5);
            int g = c.ground(cu[i], cv[i]);
            if (g == Brush.NONE || g < f - 4 || g > f + 3) {
                ch[i] = 0;
                whole[i] = false;
                continue;
            }
            if (g > f) {
                c.clearRange(cu[i], cv[i], f + 1, g);
            }
            c.plinth(cu[i], cv[i], f, Pal.COBBLE, 4);
            c.set(cu[i], f, cv[i], Pal.STONE_BRICKS);
            c.set(cu[i], f + 1, cv[i], Pal.CHISELED_BRICKS);
            for (int y = f + 2; y <= f + ch[i]; y++) {
                double r = c.noise(cu[i], y, cv[i]);
                c.set(cu[i], y, cv[i], r < 0.45 ? Pal.STONE_BRICKS : (r < 0.8 ? Pal.MOSSY_BRICKS : Pal.CRACKED_BRICKS));
            }
            int top = f + Math.max(1, ch[i]);
            if (whole[i]) {
                c.set(cu[i], top + 1, cv[i], Pal.CHISELED_BRICKS);
            } else if (rng.chance(0.5)) {
                c.set(cu[i], top + 1, cv[i], Pal.slab(Pal.MOSSY_BRICK_SLAB, SlabType.BOTTOM));
            } else {
                c.carpet(cu[i], top + 1, cv[i], Pal.MOSS_CARPET);
            }
            // ivy on the shaft
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (rng.chance(0.35)) {
                    int du = d.getStepX(), dv = d.getStepZ();
                    c.wallVines(cu[i] + du, top, cv[i] + dv, rng.range(2, Math.max(2, ch[i])));
                }
            }
            // a snapped column's drums lie in the grass beside it
            if (!whole[i] && ch[i] <= 3 && rng.chance(0.6)) {
                fallen(c, cu[i], cv[i], rng.range(3, 5), rng.nextInt(4), Pal.MOSSY_BRICKS);
            }
        }
        // lintels across neighbouring whole columns
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            if (whole[i] && whole[j]) {
                int y = f + Math.min(ch[i], ch[j]) + 2;
                c.line(cu[i], y, cv[i], cu[j], y, cv[j], Pal.MOSSY_BRICKS, false);
                if (rng.chance(0.5)) {
                    c.hangingVines((cu[i] + cu[j]) / 2, y - 1, (cv[i] + cv[j]) / 2, rng.range(1, 3));
                }
            }
        }
        // a lintel that already fell
        int lu = rng.range(-3, 3), lv = rng.range(-3, 3);
        fallenSlabs(c, lu, lv, rng.range(3, 5), rng.nextInt(4));
        // the hoard
        if (rng.chance(0.5)) {
            int g = c.ground(0, 0);
            if (g != Brush.NONE && c.free(0, g + 1, 0)) {
                c.brush.chest(c.x(0, 0), g + 1, c.z(0, 0), Direction.from2DDataValue(rng.nextInt(4)),
                        BuiltInLootTables.RUINED_PORTAL, c.site.salt());
            }
        }
    }

    /** A fallen column: {@code length} blocks lying on the ground from next to (u, v) towards local direction {@code dir}. */
    private static void fallen(SiteCtx c, int u, int v, int length, int dir, BlockState state) {
        Direction d = Direction.from2DDataValue(dir);
        for (int k = 1; k <= length; k++) {
            int uu = u + d.getStepX() * (k + 1), vv = v + d.getStepZ() * (k + 1);
            int g = c.ground(uu, vv);
            if (g == Brush.NONE || !c.free(uu, g + 1, vv)) {
                return;
            }
            c.set(uu, g + 1, vv, c.noise(uu, g, vv) < 0.5 ? state : Pal.STONE_BRICKS);
            if (c.noise(uu, g + 2, vv) < 0.3) {
                c.carpet(uu, g + 2, vv, Pal.MOSS_CARPET);
            }
        }
    }

    /** A fallen lintel: a run of slabs lying on the ground. */
    private static void fallenSlabs(SiteCtx c, int u, int v, int length, int dir) {
        Direction d = Direction.from2DDataValue(dir);
        for (int k = 0; k < length; k++) {
            int uu = u + d.getStepX() * k, vv = v + d.getStepZ() * k;
            int g = c.ground(uu, vv);
            if (g == Brush.NONE || !c.free(uu, g + 1, vv) || !c.solid(uu, g, vv)) {
                continue;
            }
            c.set(uu, g + 1, vv, Pal.slab(k % 2 == 0 ? Pal.STONE_BRICK_SLAB : Pal.MOSSY_BRICK_SLAB, SlabType.BOTTOM));
        }
    }

    /** A cone of rubble, every block standing on the ground of its own column. */
    static void rubble(SiteCtx c, int cu, int cv, int r) {
        for (int du = -r; du <= r; du++) {
            for (int dv = -r; dv <= r; dv++) {
                double dist = Math.hypot(du, dv);
                if (dist > r + 0.3) {
                    continue;
                }
                int u = cu + du, v = cv + dv;
                int g = c.ground(u, v);
                if (g == Brush.NONE) {
                    continue;
                }
                int h = (int) Math.round((r - dist) * 1.1 + c.noise(u, 7, v) * 0.9 - 0.3);
                int y = g + 1;
                for (int k = 0; k < h; k++, y++) {
                    if (!c.free(u, y, v) && !c.get(u, y, v).isAir()) {
                        break;
                    }
                    double n = c.noise(u, y, v);
                    c.set(u, y, v, n < 0.35 ? Pal.MOSSY_COBBLE : (n < 0.6 ? Pal.COBBLE : Pal.ruinBrick(n)));
                }
                if (c.solid(u, y - 1, v) && c.free(u, y, v)) {
                    double n = c.noise(u, y, v);
                    if (n < 0.35) {
                        c.set(u, y, v, Pal.slab(Pal.MOSSY_COBBLE_SLAB, SlabType.BOTTOM));
                    } else if (n < 0.6) {
                        c.carpet(u, y, v, Pal.MOSS_CARPET);
                    }
                }
            }
        }
    }

    // ============================================================================================== mossy_castle

    /**
     * A ruined keep corner: two walls (2 thick, 10-16 high) meeting at a round tower stump. The walls are whole near the
     * tower, crenellated, pierced by windows, and crumble away towards their free ends; ivy, moss, azalea and rubble.
     */
    static void mossyCastle(SiteCtx c) {
        Rng rng = c.rng;
        int f = c.floorY;
        int cu = -6, cv = -6; // the corner (tower centre); walls run +u and +v from it
        int lenA = rng.range(12, 16), lenB = rng.range(10, 14);
        int height = rng.range(10, 16);
        wall(c, cu, cv, lenA, true, height, f);
        wall(c, cu, cv, lenB, false, height - rng.range(0, 3), f);
        // the tower stump
        double tr = TOWER_RADIUS;
        int towerTop = f + rng.range(7, 11);
        for (int du = -4; du <= 4; du++) {
            for (int dv = -4; dv <= 4; dv++) {
                double dist = Math.hypot(du, dv);
                if (dist > tr + 0.2) {
                    continue;
                }
                int u = cu + du, v = cv + dv;
                int g = c.ground(u, v);
                if (g == Brush.NONE || g < f - 6) {
                    continue;
                }
                if (dist < tr - 1.2) {
                    // inside: cobble floor, air above
                    if (g > f) {
                        c.clearRange(u, v, f + 1, g);
                    }
                    c.plinth(u, v, f, Pal.COBBLE, 6);
                    c.set(u, f, v, c.noise(u, f, v) < 0.5 ? Pal.MOSSY_COBBLE : Pal.COBBLE);
                    c.clearRange(u, v, f + 1, towerTop + 2);
                    if (c.noise(u, 3, v) < 0.3) {
                        c.carpet(u, f + 1, v, Pal.MOSS_CARPET);
                    }
                    continue;
                }
                int top = towerTop - (int) Math.floor(c.noise(u, 9, v) * 4.0);
                for (int y = Math.min(g + 1, f); y <= top; y++) {
                    c.set(u, y, v, brick(c, u, y, v, f));
                }
                if (c.noise(u, 5, v) < 0.35) {
                    c.carpet(u, top + 1, v, Pal.MOSS_CARPET);
                }
            }
        }
        // tower door facing into the corner
        for (int y = f + 1; y <= f + 3; y++) {
            c.clear(cu + 3, y, cv + 3);
            c.clear(cu + 2, y, cv + 3);
            c.clear(cu + 3, y, cv + 2);
        }
        // rubble at the crumbling ends and in the yard
        rubble(c, cu + lenA - 1, cv + 3, rng.range(2, 3));
        rubble(c, cu + 3, cv + lenB - 1, rng.range(2, 3));
        rubble(c, cu + rng.range(5, 8), cv + rng.range(5, 8), 2);
    }

    /**
     * One castle wall from the tower at (cu, cv): {@code alongU} runs it along +u (thickness towards +v), else along +v.
     * Whole for the first half (crenellations on the outer row), then the top steps down raggedly towards the end.
     */
    private static void wall(SiteCtx c, int cu, int cv, int length, boolean alongU, int height, int f) {
        Rng rng = c.rng;
        double fall = height / (length * 0.55);
        int doorAt = length / 2 + rng.range(-1, 1);
        for (int s = 0; s < length; s++) {
            int broken = s < length * 0.45 ? 0 : (int) Math.round((s - length * 0.45) * fall + c.noise(s, 3, alongU ? 1 : 2) * 3.0);
            int h = height - broken;
            if (h < 1) {
                continue;
            }
            for (int t = 0; t < 2; t++) {
                int u = alongU ? cu + s : cu + t;
                int v = alongU ? cv + t : cv + s;
                if (Math.hypot(u - cu, v - cv) < TOWER_RADIUS + 0.4) {
                    continue; // the tower stands here
                }
                int g = c.ground(u, v);
                if (g == Brush.NONE || g < f - 6) {
                    continue;
                }
                int top = f + h - (broken > 0 ? (int) (c.noise(u, 4, v) * 2.0) : 0);
                for (int y = Math.min(g + 1, f); y <= top; y++) {
                    c.set(u, y, v, brick(c, u, y, v, f));
                }
                boolean outer = t == 0;
                // crenellations: merlons every other block on the outer row of the whole part
                if (broken == 0 && outer && s % 2 == 0) {
                    c.set(u, top + 1, v, brick(c, u, top + 1, v, f));
                } else if (c.noise(u, 6, v) < 0.3) {
                    c.carpet(u, top + 1, v, Pal.MOSS_CARPET);
                } else if (c.noise(u, 6, v) > 0.93) {
                    c.put(u, top + 1, v, c.noise(u, 8, v) < 0.5 ? Pal.AZALEA_LEAVES : Pal.FLOWERING_AZALEA_LEAVES);
                }
                // windows: tall slits every 4 blocks in the whole part, a doorway in the middle
                if (s % 4 == 2 && top >= f + 9) {
                    for (int y = f + 5; y <= f + 7; y++) {
                        c.clear(u, y, v);
                    }
                }
                if (s == doorAt || s == doorAt + 1) {
                    for (int y = f + 1; y <= Math.min(top - 1, f + 3); y++) {
                        c.clear(u, y, v);
                    }
                }
            }
            // ivy down both faces
            int ou = alongU ? cu + s : cu - 1, ov = alongU ? cv - 1 : cv + s;
            int iu = alongU ? cu + s : cu + 2, iv = alongU ? cv + 2 : cv + s;
            if (rng.chance(0.4)) {
                c.wallVines(ou, f + h - rng.range(0, 3), ov, rng.range(3, h));
            }
            if (rng.chance(0.3)) {
                c.wallVines(iu, f + h - rng.range(0, 3), iv, rng.range(3, h));
            }
        }
    }

    /** Castle masonry: mossier near the ground. */
    private static BlockState brick(SiteCtx c, int u, int y, int v, int f) {
        double r = c.noise(u, y, v);
        if (y <= f + 2 && r < 0.3) {
            return r < 0.12 ? Pal.MOSS_BLOCK : Pal.MOSSY_COBBLE;
        }
        return Pal.ruinBrick(r);
    }

    // =========================================================================================== overgrown_works

    /**
     * An industrial ruin: a rusted copper frame (4 posts, beams, iron-bar bracing, a fallen beam, hanging chains), a
     * crane mast with a jib and a hook chain, a rail line on a stone bed following the terrain; vines and moss all over.
     */
    static void overgrownWorks(SiteCtx c) {
        Rng rng = c.rng;
        int f = c.floorY;
        int hu = 4, hv = 3;
        int top = f + rng.range(9, 12);
        // floor: worn slabs set into the ground
        for (int u = -hu; u <= hu; u++) {
            for (int v = -hv; v <= hv; v++) {
                int g = c.ground(u, v);
                if (g == Brush.NONE || Math.abs(g - f) > 2) {
                    continue;
                }
                double r = c.noise(u, 0, v);
                if (r < 0.6) {
                    c.set(u, g, v, r < 0.25 ? Pal.POLISHED_ANDESITE : (r < 0.45 ? Pal.WE_CUT : Pal.TUFF_BRICKS));
                    c.clearPlants(u, v, g + 1, g + 1);
                }
                if (c.noise(u, 1, v) < 0.25) {
                    c.carpet(u, g + 1, v, Pal.MOSS_CARPET);
                }
            }
        }
        // posts
        int[][] posts = {{-hu, -hv}, {hu, -hv}, {-hu, hv}, {hu, hv}};
        for (int[] p : posts) {
            int g = c.ground(p[0], p[1]);
            if (g == Brush.NONE || g < f - 8) {
                continue;
            }
            for (int y = g + 1; y <= top; y++) {
                c.set(p[0], y, p[1], Pal.rust(c.noise(p[0], y, p[1])));
            }
            vinesAround(c, p[0], top, p[1], top - g);
        }
        // top beams (one long beam is broken: its middle lies on the ground)
        int gapSide = rng.nextInt(2) == 0 ? -hv : hv;
        int gap0 = rng.range(-2, 1);
        for (int u = -hu; u <= hu; u++) {
            for (int side = -1; side <= 1; side += 2) {
                int v = side * hv;
                if (v == gapSide && u >= gap0 && u <= gap0 + 2) {
                    continue;
                }
                c.set(u, top, v, Pal.rust(c.noise(u, top, v)));
                beamDressing(c, u, top, v);
            }
        }
        for (int v = -hv + 1; v < hv; v++) {
            for (int side = -1; side <= 1; side += 2) {
                c.set(side * hu, top, v, Pal.rust(c.noise(side * hu, top, v)));
                beamDressing(c, side * hu, top, v);
            }
        }
        fallenSlabsCopper(c, gap0, gapSide + (gapSide < 0 ? -1 : 1), 3);
        // mid beam and iron-bar bracing on the back side
        int mid = f + (top - f) / 2;
        for (int u = -hu + 1; u < hu; u++) {
            c.set(u, mid, hv, Pal.rust(c.noise(u, mid, hv)));
            for (int y = f + 1; y < top; y++) {
                if (y == mid || c.noise(u, y, hv + 9) < 0.3) {
                    continue;
                }
                if (c.get(u, y, hv).isAir()) {
                    c.shaped(u, y, hv, Pal.IRON_BARS);
                }
            }
        }
        // a patch of grating roof against the beams
        for (int u = -hu + 1; u < hu; u++) {
            for (int v = -hv + 1; v < hv; v++) {
                if ((Math.abs(v) == hv - 1 || Math.abs(u) == hu - 1) && c.noise(u, top, v) < 0.35) {
                    c.put(u, top, v, Pal.OX_GRATE);
                }
            }
        }
        // hanging chains from the top beams
        for (int k = 0; k < 3; k++) {
            int u = rng.range(-hu + 1, hu - 1);
            int v = rng.nextInt(2) == 0 ? -hv : hv;
            if (c.solid(u, top, v)) {
                int len = rng.range(2, 5);
                for (int y = top - 1; y >= top - len && c.get(u, y, v).isAir(); y--) {
                    c.set(u, y, v, Pal.CHAIN_Y);
                }
            }
        }
        crane(c, hu, -hv, top, f);
        rails(c, hv + 3, f);
    }

    /** Moss on top, vines hanging under a beam block. */
    private static void beamDressing(SiteCtx c, int u, int y, int v) {
        double r = c.noise(u, y + 1, v);
        if (r < 0.3) {
            c.carpet(u, y + 1, v, Pal.MOSS_CARPET);
        } else if (r > 0.94) {
            c.put(u, y + 1, v, Pal.AZALEA_LEAVES);
        }
        if (c.noise(u, y - 1, v) < 0.3) {
            c.hangingVines(u, y - 1, v, 1 + (int) (c.noise(u, y - 2, v) * 5));
        }
    }

    /** Vine strands on the four faces of a post / mast whose top is at {@code top}. */
    private static void vinesAround(SiteCtx c, int u, int top, int v, int maxLen) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (c.rng.chance(0.55)) {
                c.wallVines(u + d.getStepX(), top - c.rng.range(0, 2), v + d.getStepZ(), c.rng.range(3, Math.max(3, maxLen)));
            }
        }
    }

    /** The fallen middle of a broken beam: copper slabs lying on the ground. */
    private static void fallenSlabsCopper(SiteCtx c, int u0, int v, int length) {
        for (int k = 0; k < length; k++) {
            int u = u0 + k;
            int g = c.ground(u, v);
            if (g == Brush.NONE || !c.free(u, g + 1, v) || !c.solid(u, g, v)) {
                continue;
            }
            c.set(u, g + 1, v, Pal.slab(k % 2 == 0 ? Pal.OX_CUT_SLAB : Pal.WE_CUT_SLAB, SlabType.BOTTOM));
        }
    }

    /**
     * A crane: the frame post at (u, v) carries on as a mast 6 blocks above the frame, a jib runs 9-12 blocks out
     * along -v with an iron-bar tie, a short counter-jib with a weight, and a chain hangs from the jib tip.
     */
    private static void crane(SiteCtx c, int u, int v, int frameTop, int f) {
        Rng rng = c.rng;
        int g = c.ground(u, v);
        if (g == Brush.NONE || g < f - 8) {
            return;
        }
        int mast = frameTop + 6;
        for (int y = frameTop + 1; y <= mast; y++) {
            c.set(u, y, v, Pal.rust(c.noise(u, y, v)));
        }
        for (int y = mast + 1; y <= mast + 3; y++) {
            c.shaped(u, y, v, Pal.IRON_BARS);
        }
        int jib = rng.range(9, 12);
        for (int k = 1; k <= jib; k++) {
            c.set(u, mast, v - k, k == jib ? Pal.IRON_BLOCK : Pal.rust(c.noise(u, mast, v - k)));
            beamDressing(c, u, mast, v - k);
        }
        // the tie from the mast head down to the jib tip
        for (int k = 1; k < jib; k++) {
            int y = mast + 3 - (int) Math.round(2.0 * k / jib);
            if (y > mast && c.get(u, y, v - k).isAir()) {
                c.shaped(u, y, v - k, Pal.IRON_BARS);
            }
        }
        // counter-jib with its weight
        for (int k = 1; k <= 3; k++) {
            c.set(u, mast, v + k, Pal.rust(c.noise(u, mast, v + k)));
        }
        c.set(u, mast - 1, v + 3, Pal.IRON_BLOCK);
        c.set(u, mast - 2, v + 3, Pal.OX_COPPER);
        // the hook chain
        int drop = rng.range(5, Math.max(6, mast - f - 4));
        for (int y = mast - 1; y >= mast - drop && c.get(u, y, v - jib).isAir(); y--) {
            c.set(u, y, v - jib, Pal.CHAIN_Y);
        }
        vinesAround(c, u, mast, v, mast - g);
    }

    /** A rail line along u at row v on a stone bed, following the terrain one block at a time; rusted gaps. */
    private static void rails(SiteCtx c, int v, int f) {
        int half = 8;
        int[] g = new int[2 * half + 1];
        for (int i = 0; i <= 2 * half; i++) {
            g[i] = c.ground(i - half, v);
        }
        Direction fwd = c.frame.fwd(), back = c.frame.back();
        RailShape flat = fwd.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        for (int i = 0; i <= 2 * half; i++) {
            int u = i - half;
            if (g[i] == Brush.NONE || Math.abs(g[i] - f) > 4) {
                continue;
            }
            // the bed
            c.set(u, g[i], v, c.noise(u, g[i], v) < 0.5 ? Pal.TUFF_BRICKS : Pal.POLISHED_ANDESITE);
            for (int side = -1; side <= 1; side += 2) {
                // ballast shoulders, only where the neighbouring ground is level with the bed
                if (c.ground(u, v + side) == g[i]) {
                    c.set(u, g[i], v + side, side < 0 ? Pal.COBBLE : Pal.MOSSY_COBBLE);
                }
            }
            if (c.noise(u, 11, v) < 0.15 || !c.free(u, g[i] + 1, v)) {
                continue; // rusted away
            }
            RailShape shape = flat;
            if (i < 2 * half && g[i + 1] == g[i] + 1) {
                shape = ascending(fwd);
            } else if (i > 0 && g[i - 1] == g[i] + 1) {
                shape = ascending(back);
            }
            c.set(u, g[i] + 1, v, Pal.RAIL.setValue(RailBlock.SHAPE, shape));
        }
    }

    private static RailShape ascending(Direction towards) {
        return switch (towards) {
            case NORTH -> RailShape.ASCENDING_NORTH;
            case SOUTH -> RailShape.ASCENDING_SOUTH;
            case EAST -> RailShape.ASCENDING_EAST;
            default -> RailShape.ASCENDING_WEST;
        };
    }
}
