package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld.Span;

/**
 * Draws El'ysium's crossings (bridges and giant chains, see {@link Span}) into one chunk.
 *
 * <p>Every chunk draws only its own 16x16 columns and works each block out from the span alone (plus stateless position
 * hashes), so the pieces drawn by neighbouring chunks meet exactly, in whatever order the chunks generate.
 *
 * <p>Kinds:
 * <ul>
 *   <li>{@code STONE_ARCH}: Arganite-brick deck (walkway {@code width}, default 5) rising in a gentle hump, wall railings
 *       on raised curbs, a lantern on the railing every 16 blocks, under it a solid barrel arch whose underside is an
 *       elliptical intrados with alternating voussoirs and a keystone at the crown, vines hanging off the faces.</li>
 *   <li>{@code ROPE}: spruce/oak slab deck (default 3) hanging in a catenary ({@code sag}), log stringers with fence
 *       railings and taller posts every 6 blocks, crossbeams underneath, vine drapes.</li>
 *   <li>{@code VINE_CHAIN}: mossy stone slab deck (default 3) in a catenary, tuff brick stringers, two chain handrails on
 *       vertical chain hangers, moss and long vines.</li>
 *   <li>{@code GIANT_CHAIN}: a huge chain of 9-block diamond links in alternating planes, tuff bricks, hanging in
 *       a catenary between the anchors under the two cliffs; moss on top of the links, vines under them.</li>
 * </ul>
 * Bridge anchors are the deck block at each coast; the deck carries on {@value #ABUT} blocks into the island as a flat
 * abutment on a foundation, with headroom carved through any terrain in the way. Nothing is ever placed below
 * {@link ElysiumWorld#LOWEST_SOLID} (the brush clips it, and sags are clamped so decks never reach it).
 */
public final class SpanBuilder {
    /** Blocks the deck runs on past each anchor, into the island. */
    static final int ABUT = 6;
    /** Air carved above walkways. */
    private static final int HEADROOM = 3;
    /** Giant chain link: half the diamond's diagonal (a 9-block link). */
    private static final int LINK = 4;
    /** Links overlap by a quarter so each tip threads through its neighbour's eye. */
    private static final double LINK_SPACING = 1.5 * LINK;
    /** How far giant chains reach past their anchors into the islands' undersides. */
    private static final double CHAIN_EMBED = 6.0;

    private SpanBuilder() {}

    /** Entry point called by the chunk generator after biome decoration. Draws the part of each span inside {@code chunk}. */
    public static void build(WorldGenLevel level, ChunkPos chunk, ElysiumWorld world, List<ElysiumWorld.Span> spans) {
        if (spans == null || spans.isEmpty()) {
            return;
        }
        Brush brush = Brush.chunk(level, chunk);
        for (Span span : spans) {
            if (span == null || span.kind() == null) {
                continue;
            }
            switch (span.kind()) {
                case STONE_ARCH -> new Deck(brush, span, 5).stoneArch();
                case ROPE -> new Deck(brush, span, 3).rope();
                case VINE_CHAIN -> new Deck(brush, span, 3).vineChain();
                case GIANT_CHAIN -> giantChain(brush, span);
            }
        }
    }

    /** A stable per-span salt (spans have no id: hash their geometry). */
    static long salt(Span s) {
        long h = Rng.hash(s.kind().ordinal(), s.ax(), s.ay(), s.az());
        return Rng.hash(h, s.bx(), s.by(), s.bz());
    }

    // ------------------------------------------------------------------------------------------------ walkable decks

    /** The shared deck geometry of the three walkable bridge kinds. */
    private static final class Deck {
        final Brush b;
        final Span s;
        final Shapes.LineFrame line;
        final double half, band;
        final long salt;
        final double hump, sag;

        Deck(Brush b, Span s, int defaultWidth) {
            this.b = b;
            this.s = s;
            this.line = new Shapes.LineFrame(s.ax() + 0.5, s.az() + 0.5, s.bx() + 0.5, s.bz() + 0.5);
            int w = s.width() > 0 ? s.width() : defaultWidth;
            this.half = w / 2.0;
            this.band = line.railBand();
            this.salt = salt(s);
            this.hump = s.kind() == Span.Kind.STONE_ARCH ? Math.min(6.0, 1.5 + line.len * 0.035) : 0.0;
            // a hanging deck must never sag into the cloud layer
            double lowEnd = Math.min(s.ay(), s.by());
            double maxSag = Math.max(0.0, lowEnd - (ElysiumWorld.LOWEST_SOLID + 4));
            this.sag = s.kind() == Span.Kind.STONE_ARCH ? 0.0 : Shapes.clamp(s.sag(), 0.0, maxSag);
        }

        /** Walking surface height (in blocks, fractional) at distance {@code t} along the span; flat past the anchors. */
        double surface(double t) {
            if (t <= 0) {
                return s.ay() + 1.0;
            }
            if (t >= line.len) {
                return s.by() + 1.0;
            }
            double f = t / line.len;
            return Shapes.lerp(s.ay(), s.by(), f) + 1.0 + hump * Shapes.hump(f) + sag * Shapes.catenary(f);
        }

        /** Surface rounded to half blocks, in half-block units (so decks step by slabs). */
        int halfSteps(double t) {
            return (int) Math.round(surface(t) * 2.0);
        }

        boolean onAbutment(double t) {
            return t < 0.0 || t > line.len;
        }

        /** Visits every column of this chunk that belongs to the deck or its rails. */
        void forColumns(ColumnVisitor visitor) {
            for (int x = b.minX; x <= b.maxX; x++) {
                for (int z = b.minZ; z <= b.maxZ; z++) {
                    double t = line.along(x + 0.5, z + 0.5);
                    if (t < -ABUT || t > line.len + ABUT) {
                        continue;
                    }
                    double d = line.across(x + 0.5, z + 0.5);
                    double ad = Math.abs(d);
                    if (ad > half + band) {
                        continue;
                    }
                    visitor.visit(x, z, t, d, ad > half);
                }
            }
        }

        /** True if a railing column at distance t carries a lantern / post (every {@code every} blocks, not at the ends). */
        boolean every(double t, double every, double margin) {
            if (t < margin || t > line.len - margin) {
                return false;
            }
            return Math.abs(t - every * Math.round(t / every)) < 0.5;
        }

        /** Abutment foundation and headroom carving shared by all decks. */
        void abutment(int x, int z, int underY, int walkY, BlockState fill) {
            b.foundation(x, underY, z, fill, 10);
            for (int y = walkY; y < walkY + HEADROOM; y++) {
                b.clear(x, y, z);
            }
        }

        /** Carve headroom where terrain or foliage sits on the walkway (coast ends of the span itself). */
        void headroom(int x, int z, int walkY) {
            for (int y = walkY; y < walkY + HEADROOM; y++) {
                if (!b.get(x, y, z).isAir()) {
                    b.clear(x, y, z);
                }
            }
        }

        Direction hangSide(int x, int z) {
            return Direction.from2DDataValue((int) (Rng.hash(salt, x, 7, z) & 3));
        }

        // ---- STONE_ARCH ----------------------------------------------------------------------------------------

        void stoneArch() {
            double rise = Math.min(18.0, 3.0 + line.len * 0.2);
            forColumns((x, z, t, d, rail) -> {
                int hs = halfSteps(t);
                int topFull = Math.floorDiv(hs, 2) - 1;
                boolean halfStep = (hs & 1) == 1;
                int walkY = halfStep ? topFull + 2 : topFull + 1; // first air block above the walking surface
                double r = Rng.unit(salt, x, 1, z);
                // deck: two courses of bricks (+ a slab for half steps; rails get a full curb instead)
                BlockState face = rail ? Pal.ARG : Pal.ARG_BRICKS;
                b.set(x, topFull, z, face);
                b.set(x, topFull - 1, z, Pal.ARG_BRICKS);
                int curbTop = topFull;
                if (halfStep) {
                    if (rail) {
                        b.set(x, topFull + 1, z, Pal.ARG);
                        curbTop = topFull + 1;
                    } else {
                        b.set(x, topFull + 1, z, Pal.slab(Pal.ARG_BRICK_SLAB, SlabType.BOTTOM));
                    }
                }
                if (rail) {
                    b.shaped(x, curbTop + 1, z, Pal.ARG_BRICK_WALL);
                    if (every(t, 16.0, 3.0)) {
                        b.set(x, curbTop + 2, z, Pal.LANTERN);
                    }
                    for (int y = curbTop + 2 + (every(t, 16.0, 3.0) ? 1 : 0); y < curbTop + 1 + HEADROOM; y++) {
                        if (!b.get(x, y, z).isAir()) {
                            b.clear(x, y, z);
                        }
                    }
                } else if (onAbutment(t)) {
                    abutment(x, z, topFull - 2, walkY, Pal.ARG_BRICKS);
                } else {
                    headroom(x, z, walkY);
                }
                if (onAbutment(t)) {
                    if (rail) {
                        b.foundation(x, topFull - 2, z, Pal.ARG_BRICKS, 10);
                    }
                    return;
                }
                // the arch: elliptical intrados, springing `rise` below the deck at the anchors, crown just under it
                double f = t / line.len;
                double e = 2.0 * f - 1.0;
                int bottom = topFull - 2 - (int) Math.round(rise * (1.0 - Math.sqrt(Math.max(0.0, 1.0 - e * e))));
                boolean crown = Math.abs(t - line.len / 2.0) < 1.0;
                if (crown) {
                    bottom -= 1;
                }
                for (int y = topFull - 2; y > bottom; y--) {
                    b.put(x, y, z, rail ? Pal.ARG : Pal.ARG_BRICKS);
                }
                // voussoirs along the intrados, keystone at the crown
                BlockState vous = crown ? Pal.ARG : ((((int) Math.floor(t / 2.0)) & 1) == 0 ? Pal.ARG : Pal.ARG_BRICKS);
                b.put(x, bottom, z, vous);
                if (rail && r < 0.22) {
                    b.hangingVines(x, bottom - 1, z, 1 + (int) (Rng.unit(salt, x, 2, z) * 5), hangSide(x, z));
                }
            });
        }

        // ---- ROPE ----------------------------------------------------------------------------------------------

        void rope() {
            Direction.Axis along = line.alongX() ? Direction.Axis.X : Direction.Axis.Z;
            Direction.Axis cross = line.alongX() ? Direction.Axis.Z : Direction.Axis.X;
            forColumns((x, z, t, d, rail) -> {
                int hs = halfSteps(t);
                int slabY = Math.floorDiv(hs - 1, 2);
                SlabType type = (hs & 1) == 1 ? SlabType.BOTTOM : SlabType.TOP;
                double r = Rng.unit(salt, x, 1, z);
                if (!rail) {
                    boolean missing = !onAbutment(t) && Math.abs(d) > 0.6 && r < 0.04;
                    if (!missing) {
                        b.set(x, slabY, z, Pal.slab(r < 0.8 ? Pal.SPRUCE_SLAB : Pal.OAK_SLAB, type));
                    }
                    // crossbeam under the deck every 4 blocks
                    if (!onAbutment(t) && Math.abs(t - 4.0 * Math.round(t / 4.0)) < 0.5) {
                        b.put(x, slabY - 1, z, Pal.pillar(Pal.SPRUCE_LOG, cross));
                    } else if (!onAbutment(t) && r > 0.9) {
                        b.hangingVines(x, slabY - 1, z, 1 + (int) (Rng.unit(salt, x, 2, z) * 4), hangSide(x, z));
                    }
                    if (onAbutment(t)) {
                        abutment(x, z, slabY - 1, slabY + 1, Pal.COBBLE);
                    } else {
                        headroom(x, z, slabY + 1);
                    }
                    return;
                }
                // rail: log stringer + fence; taller posts every 6 blocks and at the ends of the span
                b.set(x, slabY, z, Pal.pillar(Pal.SPRUCE_LOG, along));
                boolean endPost = Math.abs(t) < 0.75 || Math.abs(t - line.len) < 0.75;
                boolean post = endPost || every(t, 6.0, 2.0);
                if (endPost) {
                    for (int y = slabY + 1; y <= slabY + 3; y++) {
                        b.set(x, y, z, Pal.pillar(Pal.SPRUCE_LOG, Direction.Axis.Y));
                    }
                    b.set(x, slabY + 4, z, Pal.LANTERN);
                } else {
                    b.shaped(x, slabY + 1, z, Pal.SPRUCE_FENCE);
                    if (post) {
                        b.shaped(x, slabY + 2, z, Pal.SPRUCE_FENCE);
                    }
                }
                if (onAbutment(t)) {
                    b.foundation(x, slabY - 1, z, Pal.COBBLE, 10);
                } else if (r < 0.3) {
                    b.hangingVines(x, slabY - 1, z, 2 + (int) (Rng.unit(salt, x, 2, z) * 5), hangSide(x, z));
                }
            });
        }

        // ---- VINE_CHAIN ----------------------------------------------------------------------------------------

        void vineChain() {
            Direction.Axis along = line.alongX() ? Direction.Axis.X : Direction.Axis.Z;
            forColumns((x, z, t, d, rail) -> {
                int hs = halfSteps(t);
                int slabY = Math.floorDiv(hs - 1, 2);
                SlabType type = (hs & 1) == 1 ? SlabType.BOTTOM : SlabType.TOP;
                double r = Rng.unit(salt, x, 1, z);
                if (!rail) {
                    BlockState deck = r < 0.55 ? Pal.MOSSY_BRICK_SLAB : (r < 0.85 ? Pal.MOSSY_COBBLE_SLAB : Pal.ARG_SLAB);
                    b.set(x, slabY, z, Pal.slab(deck, type));
                    if (type == SlabType.TOP && Rng.unit(salt, x, 3, z) < 0.25) {
                        b.carpet(x, slabY + 1, z, Pal.MOSS_CARPET);
                    }
                    if (onAbutment(t)) {
                        abutment(x, z, slabY - 1, slabY + 1, Pal.MOSSY_COBBLE);
                    } else {
                        headroom(x, z, slabY + 1);
                        if (r > 0.88) {
                            b.hangingVines(x, slabY - 1, z, 2 + (int) (Rng.unit(salt, x, 2, z) * 6), hangSide(x, z));
                        }
                    }
                    return;
                }
                // rail: stringer, chain hanger every 3 blocks, horizontal chain handrail on top
                b.set(x, slabY, z, Pal.CHAIN_LINK);
                boolean hanger = Math.abs(t) < 0.75 || Math.abs(t - line.len) < 0.75 || every(t, 3.0, 0.0);
                if (hanger) {
                    b.set(x, slabY + 1, z, Pal.CHAIN_Y);
                    b.set(x, slabY + 2, z, Pal.CHAIN_LINK);
                } else {
                    b.set(x, slabY + 2, z, Pal.chain(along));
                }
                if (onAbutment(t)) {
                    b.foundation(x, slabY - 1, z, Pal.MOSSY_COBBLE, 10);
                } else if (r < 0.4) {
                    b.hangingVines(x, slabY - 1, z, 2 + (int) (Rng.unit(salt, x, 2, z) * 7), hangSide(x, z));
                }
            });
        }
    }

    @FunctionalInterface
    private interface ColumnVisitor {
        /** (x, z) block column, t distance along the span from A, d signed distance across, rail = outside the walkway. */
        void visit(int x, int z, double t, double d, boolean rail);
    }

    // ------------------------------------------------------------------------------------------------- giant chain

    /**
     * The giant chain: diamond links in alternating planes along a catenary from A to B (both extended a little into the
     * islands). Each chunk walks the whole curve (cheap: a few hundred samples) and draws only links near it.
     */
    private static void giantChain(Brush b, Span s) {
        long salt = salt(s);
        double dx = s.bx() - s.ax(), dz = s.bz() - s.az(), len = Math.max(1.0, Math.hypot(dx, dz));
        double ux = dx / len, uz = dz / len;
        double ax = s.ax() + 0.5 - ux * CHAIN_EMBED, az = s.az() + 0.5 - uz * CHAIN_EMBED, ay = s.ay() + 0.5;
        double bx = s.bx() + 0.5 + ux * CHAIN_EMBED, bz = s.bz() + 0.5 + uz * CHAIN_EMBED, by = s.by() + 0.5;
        // the lowest link must stay well above the cloud layer
        double floor = ElysiumWorld.LOWEST_SOLID + LINK + 3;
        double sag = Shapes.clamp(s.sag(), 0.0, Math.max(0.0, Math.min(ay, by) - floor));
        int n = Math.max(32, (int) (len / 2.0));
        double[][] pts = new double[n + 1][3];
        double[] arc = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double f = i / (double) n;
            pts[i][0] = Shapes.lerp(ax, bx, f);
            pts[i][1] = Shapes.lerp(ay, by, f) + sag * Shapes.catenary(f);
            pts[i][2] = Shapes.lerp(az, bz, f);
            if (i > 0) {
                arc[i] = arc[i - 1] + Math.sqrt(sq(pts[i][0] - pts[i - 1][0]) + sq(pts[i][1] - pts[i - 1][1]) + sq(pts[i][2] - pts[i - 1][2]));
            }
        }
        drawLinks(b, salt, pts, arc, n);
    }

    /**
     * Loose chains, tied to one island only: from under an island straight down, or from the coast out over the edge and
     * down. Same links as the giant chains, drawn the same way, one chunk at a time.
     */
    public static void buildHanging(WorldGenLevel level, ChunkPos chunk, List<ElysiumWorld.HangingChain> chains) {
        if (chains.isEmpty()) {
            return;
        }
        Brush brush = Brush.chunk(level, chunk);
        for (ElysiumWorld.HangingChain h : chains) {
            hangingChain(brush, h);
        }
    }

    private static void hangingChain(Brush b, ElysiumWorld.HangingChain h) {
        double ax = h.x() + 0.5 - h.outX() * CHAIN_EMBED, az = h.z() + 0.5 - h.outZ() * CHAIN_EMBED;
        double ay = h.y() + (h.run() == 0 ? CHAIN_EMBED : 0.5);
        double total = h.run() + CHAIN_EMBED;
        double phase = Rng.unit(h.salt(), 1, 2, 3) * Math.PI * 2;
        int n = Math.max(16, (int) ((h.drop() + total) / 2.0));
        double[][] pts = new double[n + 1][3];
        double[] arc = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double f = i / (double) n;
            // swings out over the first third, then plumb; the sway dies away towards the anchor
            double k = 1 - Math.min(1.0, f * 3.0);
            double out = total * (1 - k * k * k);
            double sway = 1.5 * f * Math.sin(f * 5.0 + phase);
            pts[i][0] = ax + h.outX() * out + sway;
            pts[i][1] = ay - (h.drop() + ay - h.y()) * Math.pow(f, h.run() == 0 ? 1.0 : 1.5);
            pts[i][2] = az + h.outZ() * out + sway * 0.6;
            if (i > 0) {
                arc[i] = arc[i - 1] + Math.sqrt(sq(pts[i][0] - pts[i - 1][0]) + sq(pts[i][1] - pts[i - 1][1]) + sq(pts[i][2] - pts[i - 1][2]));
            }
        }
        drawLinks(b, h.salt(), pts, arc, n);
    }

    /** Diamond links in alternating planes along a polyline with cumulative lengths {@code arc}, plus moss and vines. */
    private static void drawLinks(Brush b, long salt, double[][] pts, double[] arc, int n) {
        int links = (int) (arc[n] / LINK_SPACING);
        int seg = 0;
        double reach = LINK + 2.0;
        // collected for the dressing pass: (x, y, z) of every block this chunk placed
        int[] placed = new int[1024 * 3];
        int count = 0;
        for (int k = 0; k < links; k++) {
            double at = (k + 0.5) * LINK_SPACING;
            while (seg < n - 1 && arc[seg + 1] < at) {
                seg++;
            }
            double segLen = Math.max(1e-6, arc[seg + 1] - arc[seg]);
            double f = (at - arc[seg]) / segLen;
            double cx = pts[seg][0] + (pts[seg + 1][0] - pts[seg][0]) * f;
            double cy = pts[seg][1] + (pts[seg + 1][1] - pts[seg][1]) * f;
            double cz = pts[seg][2] + (pts[seg + 1][2] - pts[seg][2]) * f;
            if (cx < b.minX - reach || cx > b.maxX + 1 + reach || cz < b.minZ - reach || cz > b.maxZ + 1 + reach) {
                continue;
            }
            double tx = pts[seg + 1][0] - pts[seg][0], ty = pts[seg + 1][1] - pts[seg][1], tz = pts[seg + 1][2] - pts[seg][2];
            double tl = Math.max(1e-6, Math.sqrt(tx * tx + ty * ty + tz * tz));
            tx /= tl;
            ty /= tl;
            tz /= tl;
            // h: horizontal side vector; v = t x h. Even links lie in the (t, h) plane, odd links in (t, v).
            double hx = -tz, hz = tx, hl = Math.hypot(hx, hz);
            if (hl < 0.2) { // running straight down: any horizontal side will do, and a fixed one keeps the links aligned
                hx = 1.0;
                hz = 0.0;
                hl = 1.0;
            }
            hx /= hl;
            hz /= hl;
            double vx = ty * hz, vy = tz * hx - tx * hz, vz = -ty * hx;
            double nx, ny, nz, wx2, wy2, wz2;
            if ((k & 1) == 0) {
                nx = hx; ny = 0.0; nz = hz;
                wx2 = vx; wy2 = vy; wz2 = vz;
            } else {
                nx = vx; ny = vy; nz = vz;
                wx2 = hx; wy2 = 0.0; wz2 = hz;
            }
            BlockState material = (k % 7 == 3) ? Pal.CHAIN_LINK_ACCENT : Pal.CHAIN_LINK;
            int steps = LINK * 6;
            for (int edge = 0; edge < 4; edge++) {
                double[] p = DIAMOND[edge], q = DIAMOND[(edge + 1) % 4];
                for (int i = 0; i < steps; i++) {
                    double u = i / (double) steps;
                    double sAlong = (p[0] + (q[0] - p[0]) * u) * LINK, sAcross = (p[1] + (q[1] - p[1]) * u) * LINK;
                    for (int thick = 0; thick < 2; thick++) {
                        double px = cx + tx * sAlong + nx * sAcross + wx2 * (thick - 0.5);
                        double py = cy + ty * sAlong + ny * sAcross + wy2 * (thick - 0.5);
                        double pz = cz + tz * sAlong + nz * sAcross + wz2 * (thick - 0.5);
                        int ix = (int) Math.floor(px), iy = (int) Math.floor(py), iz = (int) Math.floor(pz);
                        if (b.inside(ix, iy, iz) && b.put(ix, iy, iz, material)) {
                            if (count + 3 > placed.length) {
                                placed = java.util.Arrays.copyOf(placed, placed.length * 2);
                            }
                            placed[count++] = ix;
                            placed[count++] = iy;
                            placed[count++] = iz;
                        }
                    }
                }
            }
        }
        // dressing: moss on top of the links, vines trailing from their undersides
        for (int i = 0; i < count; i += 3) {
            int x = placed[i], y = placed[i + 1], z = placed[i + 2];
            double r = Rng.unit(salt, x, y, z);
            if (r < 0.16) {
                b.carpet(x, y + 1, z, Pal.MOSS_CARPET);
            } else if (r > 0.95 && b.get(x, y - 1, z).isAir()) {
                b.hangingVines(x, y - 1, z, 2 + (int) (Rng.unit(salt, z, y, x) * 6),
                        Direction.from2DDataValue((int) (Rng.hash(salt, x, y, z) >>> 60) & 3));
            }
        }
    }

    /** Diamond corners in (along, across), unit size. */
    private static final double[][] DIAMOND = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    private static double sq(double v) {
        return v * v;
    }
}
