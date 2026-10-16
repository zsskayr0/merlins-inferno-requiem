package dev.zsskayr.merlins_inferno.entity.cinder;

/** Heat is independent of health: a tail tip in lava cannot reset the clock. */
public final class CinderVitals {
    public static final int COLD_LIMIT = 600;
    public static final int DAMAGE_START = 400;
    public static final int SOAK_DELAY = 60;
    private int coldTicks;
    private int soakedTicks;

    public void tick(double immersedFraction, boolean fullySubmerged) {
        soakedTicks = fullySubmerged ? (soakedTicks >= SOAK_DELAY + 20 ? SOAK_DELAY + 1 : soakedTicks + 1) : 0;
        coldTicks = immersedFraction >= 0.25 ? Math.max(0, coldTicks - 4) : Math.min(COLD_LIMIT, coldTicks + 1);
    }

    public int coldTicks() { return coldTicks; }
    public int soakedTicks() { return soakedTicks; }
    public boolean shouldHeal() { return soakedTicks >= SOAK_DELAY && soakedTicks % 20 == 0; }
    public boolean shouldFreezeDamage() { return coldTicks >= DAMAGE_START && coldTicks % 20 == 0; }
    public boolean isTerminal() { return coldTicks >= COLD_LIMIT; }
    public float damageMultiplier() { return 1.0F - 0.7F * coldTicks / COLD_LIMIT; }
    public void restore(int cold, int soak) {
        coldTicks = Math.max(0, Math.min(COLD_LIMIT, cold));
        // Loading never grants a free healing tick; full immersion is rechecked.
        soakedTicks = Math.max(0, Math.min(1200, soak));
    }
}
