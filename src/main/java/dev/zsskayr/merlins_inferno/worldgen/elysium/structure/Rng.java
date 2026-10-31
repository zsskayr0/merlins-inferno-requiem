package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

/**
 * Tiny deterministic random source for the structure builders. Two flavours:
 * <ul>
 *   <li>a sequential stream ({@link #nextInt}, {@link #nextDouble}...) seeded from a site's salt - used by
 *       {@link SiteBuilder}, which runs once per site in a fixed order, so the same salt always builds the same thing;</li>
 *   <li>stateless position hashes ({@link #hash}, {@link #unit}) - used by {@link SpanBuilder}, where each chunk draws
 *       its own slice of a span and the answer for a block must not depend on which chunk asks first.</li>
 * </ul>
 * SplitMix64 underneath; no Minecraft types, no global state, thread-safe per instance.
 */
final class Rng {
    private long state;

    Rng(long seed) {
        this.state = mix(seed ^ 0x6A09E667F3BCC909L);
    }

    /** SplitMix64 finaliser: a good 64-bit avalanche. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Stateless hash of a salt and up to three integers. */
    static long hash(long salt, int a, int b, int c) {
        long h = mix(salt + 0x9E3779B97F4A7C15L);
        h = mix(h ^ (a * 0x632BE59BD9B4E019L));
        h = mix(h ^ (b * 0x85157AF5L));
        h = mix(h ^ (c * 0xC2B2AE3D27D4EB4FL));
        return h;
    }

    /** Stateless uniform double in [0, 1) for (salt, a, b, c). */
    static double unit(long salt, int a, int b, int c) {
        return (hash(salt, a, b, c) >>> 11) * 0x1.0p-53;
    }

    long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        return mix(state);
    }

    double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    /** Uniform int in [0, bound). bound must be positive. */
    int nextInt(int bound) {
        return (int) Math.floorMod(nextLong(), (long) bound);
    }

    /** Uniform int in [min, max] inclusive. */
    int range(int min, int max) {
        return min + nextInt(Math.max(1, max - min + 1));
    }

    boolean chance(double p) {
        return nextDouble() < p;
    }

    /** One of the given items, uniformly. */
    @SafeVarargs
    final <T> T pick(T... items) {
        return items[nextInt(items.length)];
    }
}
