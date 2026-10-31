package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

/**
 * Pure geometry used by the builders (no Minecraft types): curves for hanging and arched spans, and a straight
 * "line frame" that turns world (x, z) into (along, across) coordinates relative to a segment.
 */
final class Shapes {
    private Shapes() {}

    /** Stiffness of {@link #catenary}: higher = flatter middle, steeper ends. 2 looks like a real hanging rope. */
    private static final double CATENARY_K = 2.0;
    private static final double COSH_K = Math.cosh(CATENARY_K);

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /**
     * Normalised catenary: 0 at f = 0 and f = 1, -1 at f = 0.5. Multiply by the sag and add to the straight line
     * between the anchors to get a hanging curve.
     */
    static double catenary(double f) {
        return (Math.cosh(CATENARY_K * (2.0 * f - 1.0)) - COSH_K) / (COSH_K - 1.0);
    }

    /** Arch hump: 0 at both ends, 1 in the middle (a half sine, gentler than a parabola at the crown). */
    static double hump(double f) {
        return Math.sin(Math.PI * clamp(f, 0.0, 1.0));
    }

    /**
     * A straight segment from A to B seen from above. {@link #along} is the distance from A projected on AB (0 at A,
     * {@link #len} at B), {@link #across} the signed distance from the centre line (positive to the right walking A->B
     * on a map where +z is south).
     */
    static final class LineFrame {
        final double ax, az, ux, uz, len;

        LineFrame(double ax, double az, double bx, double bz) {
            this.ax = ax;
            this.az = az;
            double dx = bx - ax, dz = bz - az;
            double l = Math.hypot(dx, dz);
            this.len = Math.max(1e-6, l);
            this.ux = l < 1e-6 ? 1.0 : dx / l;
            this.uz = l < 1e-6 ? 0.0 : dz / l;
        }

        double along(double x, double z) {
            return (x - ax) * ux + (z - az) * uz;
        }

        double across(double x, double z) {
            return -(x - ax) * uz + (z - az) * ux;
        }

        /**
         * Width of a band that is 1 block wide perpendicular to the line but still 4-connected on the block grid
         * (a diagonal line needs a wider band, up to sqrt 2).
         */
        double railBand() {
            return Math.max(1.0, Math.abs(ux) + Math.abs(uz)) * 0.999;
        }

        /** True when the line runs mostly along x. */
        boolean alongX() {
            return Math.abs(ux) >= Math.abs(uz);
        }
    }
}
