package dev.zsskayr.merlins_inferno.entity.cinder;

public enum CinderAction {
    SWIM("swim", 120, true), FAST("swim_fast", 32, true), LEAP("leap_attack", 33, false),
    DASH("lava_dash", 21, false), HURT("hurt", 14, false), RISE("land_rise", 48, false),
    LAND_IDLE("land_idle", 96, true), LAND_ATTACK("land_attack", 30, false),
    COOLING("cooling", 600, false), DEATH("death", 56, false);

    public final String clip;
    public final int ticks;
    public final boolean loop;
    CinderAction(String clip, int ticks, boolean loop) { this.clip = clip; this.ticks = ticks; this.loop = loop; }
    public static CinderAction fromId(int id) { return values()[Math.max(0, Math.min(values().length - 1, id))]; }
    public boolean attacking() { return this == LEAP || this == DASH || this == LAND_ATTACK; }
    public boolean locked() { return attacking() || this == HURT || this == RISE || this == DEATH; }
}
