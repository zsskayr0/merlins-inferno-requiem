package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * El'ysium's white architecture - clean, bright, unruined, the counterpoint to the mossy dark-stone ruins:
 * the curved colonnade on the coast and the spire chapel with its mirror floor. Porcelain (the mod's), quartz and
 * calcite; lit by lanterns, sea lanterns under glass and end rods.
 */
final class WhiteArchitecture {
    private WhiteArchitecture() {}

    // ============================================================================================ white_colonnade

    /**
     * A curved colonnade on the coast: 9-15 quartz-pillar columns on an arc (convex side facing the site's rotation,
     * i.e. out over the clouds), a continuous white entablature with a porcelain cornice, a tiled porcelain floor raised
     * one step on a calcite plinth, a ring of stairs down on the outer edge, hanging lanterns between the columns and a
     * lit pedestal at the centre of the arc.
     */
    static void colonnade(SiteCtx c) {
        Rng rng = c.rng;
        int floor = c.floorY + 1;               // the floor block; people walk at floor + 1
        int radius = rng.range(10, 13);
        double sweep = Math.toRadians(rng.range(55, 75));
        int columns = rng.range(9, 15);
        int colH = 7;                            // floor+1 .. floor+colH: base, shaft, capital
        double centreU = -(radius - 2);          // arc centre, inland of the origin
        int architrave = floor + colH + 1;
        // floor, steps
        int reach = radius + 3;
        for (int u = (int) Math.floor(centreU) - 1; u <= reach; u++) {
            for (int v = -reach; v <= reach; v++) {
                double ru = u - centreU, rv = v;
                double dist = Math.hypot(ru, rv);
                double ang = Math.atan2(rv, ru);
                if (Math.abs(ang) > sweep + 0.14 || dist > radius + 2.3) {
                    continue;
                }
                boolean step = dist > radius + 1.2;
                int top = step ? floor - 1 : floor;
                int g = c.ground(u, v);
                if (g == Brush.NONE || g < top - 6) {
                    continue;
                }
                if (g >= top) {
                    c.clearRange(u, v, top, g);
                }
                if (!c.plinth(u, v, top, Pal.CALCITE, 6)) {
                    continue;
                }
                if (step) {
                    // a step down on the outside of the arc, rising towards the centre
                    c.shaped(u, floor - 1, v, Pal.stairs(Pal.POL_PORCELAIN_STAIRS, c.worldDir(-ru, -rv), false));
                    c.clearRange(u, v, floor, floor + 2);
                    continue;
                }
                int ring = (int) Math.floor(dist);
                c.set(u, floor, v, ring % 4 == 3 ? Pal.POL_PORCELAIN : Pal.TILED_PORCELAIN);
                // clean: nothing grows on the floor or between the columns
                c.clearRange(u, v, floor + 1, architrave + 2);
            }
        }
        // columns
        int first = -1, last = -1;
        double[] colAng = new double[columns];
        boolean[] stands = new boolean[columns];
        for (int i = 0; i < columns; i++) {
            double a = -sweep + i * (2.0 * sweep / (columns - 1));
            colAng[i] = a;
            int u = (int) Math.round(centreU + Math.cos(a) * radius);
            int v = (int) Math.round(Math.sin(a) * radius);
            if (!c.solid(u, floor, v)) {
                continue; // no floor there (coast fell away): no column
            }
            stands[i] = true;
            if (first < 0) {
                first = i;
            }
            last = i;
            c.set(u, floor + 1, v, Pal.POL_PORCELAIN);
            for (int y = floor + 2; y < floor + colH; y++) {
                c.set(u, y, v, Pal.pillar(Pal.QUARTZ_PILLAR, Direction.Axis.Y));
            }
            c.set(u, floor + colH, v, Pal.CHISELED_QUARTZ);
        }
        if (first < 0 || first == last) {
            return;
        }
        // entablature: a two-block band along the arc between the outermost standing columns
        double a0 = colAng[first] - 0.04, a1 = colAng[last] + 0.04;
        for (int u = (int) Math.floor(centreU) - 1; u <= reach; u++) {
            for (int v = -reach; v <= reach; v++) {
                double ru = u - centreU, rv = v;
                double dist = Math.hypot(ru, rv);
                double ang = Math.atan2(rv, ru);
                if (ang < a0 || ang > a1 || Math.abs(dist - radius) > 0.85) {
                    continue;
                }
                c.set(u, architrave, v, Pal.SMOOTH_QUARTZ);
                c.set(u, architrave + 1, v, Pal.slab(Pal.POL_PORCELAIN_SLAB, SlabType.BOTTOM));
            }
        }
        // lanterns hanging between neighbouring columns
        for (int i = first; i < last; i++) {
            if (!stands[i] || !stands[i + 1]) {
                continue;
            }
            double a = (colAng[i] + colAng[i + 1]) / 2.0;
            int u = (int) Math.round(centreU + Math.cos(a) * radius);
            int v = (int) Math.round(Math.sin(a) * radius);
            if (c.solid(u, architrave, v) && c.get(u, architrave - 1, v).isAir()) {
                c.set(u, architrave - 1, v, Pal.LANTERN_HANGING);
            }
        }
        // a lit pedestal where the arc is centred
        int pu = (int) Math.round(centreU + 2), pv = 0;
        if (c.solid(pu, floor, pv)) {
            c.set(pu, floor + 1, pv, Pal.CHISELED_QUARTZ);
            c.set(pu, floor + 2, pv, Pal.END_ROD_UP);
        }
    }

    // =============================================================================================== spire_chapel

    /**
     * A small white chapel: a 15 x 9 hall of polished porcelain with quartz pilasters, tall glass windows, a gabled
     * porcelain roof and a stepped front door (facing the site's rotation); 3-5 slender spires (3x3 calcite shafts
     * tapering through quartz stairs to a pillar and an end-rod tip). Inside: a mirror floor - a glass aisle over sea
     * lanterns between polished porcelain - quartz clusters along the walls, chained lanterns, a quartz altar crowned by
     * a lyrium cluster. Stands on a quartzite foundation with stilt pillars where the ground falls away.
     */
    static void spireChapel(SiteCtx c) {
        Rng rng = c.rng;
        int floor = c.floorY + 1;
        int hu = 7, hv = 4;          // hall walls at |u| == hu, |v| == hv
        int wallH = 7;               // wall from floor .. floor + wallH (cornice)
        int eave = floor + wallH;
        int spires = 3 + rng.nextInt(3);
        // foundation + floor + clear (hall, steps, spire bases)
        for (int u = -hu - 2; u <= hu + 2; u++) {
            for (int v = -hv - 2; v <= hv + 2; v++) {
                boolean hall = Math.abs(u) <= hu && Math.abs(v) <= hv;
                boolean spireBase = Math.abs(Math.abs(u) - (hu + 1)) <= 1 && Math.abs(Math.abs(v) - (hv + 1)) <= 1;
                boolean steps = u > hu && u <= hu + 2 && Math.abs(v) <= 1;
                if (!hall && !spireBase && !steps) {
                    continue;
                }
                int top = steps ? floor - (u - hu - 1) : floor;
                int g = c.ground(u, v);
                if (g != Brush.NONE && g >= top) {
                    c.clearRange(u, v, top, g);
                }
                boolean pillar = (u % 3 == 0 && v % 3 == 0) || Math.abs(u) >= hu || Math.abs(v) >= hv;
                if (!c.footing(u, v, top, Pal.QUARTZITE, Pal.QUARTZITE, 6, pillar)) {
                    continue;
                }
                if (steps) {
                    c.shaped(u, top, v, Pal.stairs(Pal.POL_PORCELAIN_STAIRS, c.frame.back(), false));
                    c.clearRange(u, v, top + 1, top + 4);
                    continue;
                }
                c.set(u, floor, v, mirrorFloor(u, v, hu, hv));
                if (Math.abs(v) <= 1 && Math.abs(u) < hu) {
                    c.set(u, floor - 1, v, Pal.SEA_LANTERN);
                }
                c.clearRange(u, v, floor + 1, eave + 6);
            }
        }
        // walls
        for (int u = -hu; u <= hu; u++) {
            for (int v = -hv; v <= hv; v++) {
                if (Math.abs(u) != hu && Math.abs(v) != hv) {
                    continue;
                }
                if (!c.solid(u, floor, v)) {
                    continue; // unsupported corner of the footprint
                }
                boolean pilaster = (Math.abs(u) == hu && Math.abs(v) == hv) || (Math.abs(v) == hv && (u + hu) % 3 == 0)
                        || (Math.abs(u) == hu && (v + hv) % 4 == 0);
                boolean door = u == hu && Math.abs(v) <= 1;
                pilaster &= !door;
                boolean window = !pilaster && Math.abs(v) == hv;
                for (int y = floor + 1; y <= eave; y++) {
                    BlockState s;
                    if (y == eave) {
                        s = Pal.SMOOTH_QUARTZ;
                    } else if (pilaster) {
                        s = Pal.pillar(Pal.QUARTZ_PILLAR, Direction.Axis.Y);
                    } else if (window && y >= floor + 2 && y <= floor + 5) {
                        c.shaped(u, y, v, Pal.WHITE_PANE);
                        continue;
                    } else if (door && y <= floor + 4) {
                        if (y == floor + 4 && v != 0) {
                            c.shaped(u, y, v, Pal.stairs(Pal.POL_PORCELAIN_STAIRS, v > 0 ? c.frame.rightDir() : c.frame.leftDir(), true));
                        } else {
                            c.clear(u, y, v);
                        }
                        continue;
                    } else {
                        s = y == floor + 1 ? Pal.TILED_PORCELAIN : Pal.POL_PORCELAIN;
                    }
                    c.set(u, y, v, s);
                }
            }
        }
        // gabled roof along u: stairs climbing from both eaves to a ridge at v == 0
        int ridge = eave + hv;
        for (int u = -hu - 1; u <= hu + 1; u++) {
            for (int v = -hv - 1; v <= hv + 1; v++) {
                int k = Math.abs(v);
                int y = ridge - k + 1;          // |v| = hv + 1 -> eave, |v| = hv -> eave + 1, ...
                if (k == 0) {
                    c.set(u, ridge, v, Pal.POL_PORCELAIN);
                    c.set(u, ridge + 1, v, Pal.slab(Pal.QUARTZ_SLAB, SlabType.BOTTOM));
                    continue;
                }
                Direction up = v > 0 ? c.frame.leftDir() : c.frame.rightDir(); // high side towards the ridge
                c.shaped(u, y, v, Pal.stairs(Pal.PORCELAIN_STAIRS, up, false));
                // gable ends: fill the triangle under the roof
                if (Math.abs(u) == hu && k <= hv - 1) {
                    for (int yy = eave + 1; yy < y; yy++) {
                        c.set(u, yy, v, Pal.POL_PORCELAIN);
                    }
                }
            }
        }
        // round window in each gable
        c.set(hu, eave + 2, 0, Pal.WHITE_GLASS);
        c.set(-hu, eave + 2, 0, Pal.WHITE_GLASS);
        for (int yy = eave + 1; yy < ridge; yy++) {
            if (yy != eave + 2) {
                c.set(hu, yy, 0, Pal.POL_PORCELAIN);
                c.set(-hu, yy, 0, Pal.POL_PORCELAIN);
            }
        }
        // spires
        int tall = rng.range(14, 17);
        boolean[] corners = switch (spires) {
            case 3 -> new boolean[] {true, true, false, false};
            default -> new boolean[] {true, true, true, true};
        };
        int[][] cornerAt = {{hu + 1, -(hv + 1)}, {hu + 1, hv + 1}, {-(hu + 1), -(hv + 1)}, {-(hu + 1), hv + 1}};
        for (int i = 0; i < 4; i++) {
            if (!corners[i] || !c.solid(cornerAt[i][0], floor, cornerAt[i][1])) {
                continue;
            }
            int h = i < 2 ? tall + 3 : tall;   // the front pair is taller
            spire(c, cornerAt[i][0], cornerAt[i][1], floor + 1, floor + h);
        }
        if (spires != 4) {
            spire(c, 0, 0, ridge, ridge + rng.range(8, 11));
        }
        // interior: quartz clusters along the walls, chained lanterns, the altar
        for (int u = -hu + 2; u <= hu - 2; u += 3) {
            for (int side = -1; side <= 1; side += 2) {
                int v = side * (hv - 1);
                if (c.solid(u, floor, v) && c.get(u, floor + 1, v).isAir()) {
                    c.set(u, floor + 1, v, Pal.QUARTZ_CLUSTER.setValue(AmethystClusterBlock.FACING, Direction.UP));
                }
            }
        }
        for (int u = -4; u <= 4; u += 4) {
            if (c.solid(u, ridge, 0) && c.get(u, ridge - 1, 0).isAir()) {
                c.set(u, ridge - 1, 0, Pal.CHAIN_Y);
                c.set(u, ridge - 2, 0, Pal.LANTERN_HANGING);
            }
        }
        int au = -hu + 1;
        if (c.solid(au, floor, 0)) {
            c.set(au, floor + 1, 0, Pal.QUARTZ_BRICKS);
            c.set(au, floor + 2, 0, Pal.LYRIUM_CLUSTER.setValue(AmethystClusterBlock.FACING, Direction.UP));
            for (int side = -1; side <= 1; side += 2) {
                c.set(au, floor + 1, side * 2, Pal.CHISELED_QUARTZ);
                c.set(au, floor + 2, side * 2, Pal.END_ROD_UP);
            }
        }
    }

    /** The mirror floor: glass aisle (lit from below), polished porcelain field, tiled border. */
    private static BlockState mirrorFloor(int u, int v, int hu, int hv) {
        if (Math.abs(u) >= hu || Math.abs(v) >= hv) {
            return Pal.TILED_PORCELAIN;
        }
        if (Math.abs(v) <= 1) {
            return Pal.WHITE_GLASS;
        }
        if (Math.abs(v) == hv - 1 || Math.abs(u) == hu - 1) {
            return Pal.TILED_PORCELAIN;
        }
        return Pal.POL_PORCELAIN;
    }

    /**
     * A slender spire centred on (u, v): a 3x3 calcite shaft from y0 to y1 with porcelain bands and slit windows, a ring
     * of quartz stairs, a 1x1 quartz pillar, a slab and an end-rod tip.
     */
    private static void spire(SiteCtx c, int u, int v, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            boolean band = (y - y0) % 5 == 4;
            for (int du = -1; du <= 1; du++) {
                for (int dv = -1; dv <= 1; dv++) {
                    boolean slit = !band && du * dv == 0 && (du != 0 || dv != 0) && (y - y0) % 5 == 2 && y > y0 + 4;
                    if (slit) {
                        c.shaped(u + du, y, v + dv, Pal.WHITE_PANE);
                    } else {
                        c.set(u + du, y, v + dv, band ? Pal.POL_PORCELAIN : Pal.CALCITE);
                    }
                }
            }
        }
        int t = y1 + 1;
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                if (du == 0 && dv == 0) {
                    continue;
                }
                // high side towards the core; corners pick one axis and the post-processing rounds them
                Direction towards = du != 0 ? c.worldDir(-du, 0) : c.worldDir(0, -dv);
                c.shaped(u + du, t, v + dv, Pal.stairs(Pal.QUARTZ_STAIRS, towards, false));
            }
        }
        for (int y = t; y <= t + 3; y++) {
            c.set(u, y, v, Pal.pillar(Pal.QUARTZ_PILLAR, Direction.Axis.Y));
        }
        c.set(u, t + 4, v, Pal.slab(Pal.QUARTZ_SLAB, SlabType.DOUBLE));
        c.set(u, t + 5, v, Pal.END_ROD_UP);
    }
}
