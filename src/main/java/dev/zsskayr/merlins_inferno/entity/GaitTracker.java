package dev.zsskayr.merlins_inferno.entity;

import net.minecraft.world.entity.Entity;

/**
 * Picks "idle" / "walk" / "run" for a GeckoLib entity from how fast it actually moves, without flicker. Remote entities
 * carry no reliable velocity, so the speed is the position change per tick; that number jumps around (network steps,
 * stop-and-go strolls), and reading it raw made models swap idle and walk from one frame to the next. Here the speed is
 * smoothed once per tick and each gait has a higher threshold to enter than to leave, so a gait only changes when the
 * movement really did.
 */
public final class GaitTracker {
    /** Speeds in blocks per tick. */
    private static final double WALK_ENTER = 0.045, WALK_EXIT = 0.026;
    private static final double RUN_ENTER = 0.19, RUN_EXIT = 0.16;
    /** How much of each tick's speed enters the smoothed value. */
    private static final double SMOOTHING = 0.3;

    private double smoothed;
    private int lastTick = Integer.MIN_VALUE;
    private String gait = "idle";

    /** The gait for this entity; safe to call every frame - the speed is only sampled once per tick. */
    public String update(Entity entity) {
        if (entity.tickCount != this.lastTick) {
            this.lastTick = entity.tickCount;
            double dx = entity.getX() - entity.xOld;
            double dz = entity.getZ() - entity.zOld;
            this.smoothed += (Math.sqrt(dx * dx + dz * dz) - this.smoothed) * SMOOTHING;
            this.gait = switch (this.gait) {
                case "run" -> this.smoothed < RUN_EXIT ? (this.smoothed < WALK_EXIT ? "idle" : "walk") : "run";
                case "walk" -> this.smoothed > RUN_ENTER ? "run" : (this.smoothed < WALK_EXIT ? "idle" : "walk");
                default -> this.smoothed > RUN_ENTER ? "run" : (this.smoothed > WALK_ENTER ? "walk" : "idle");
            };
        }
        return this.gait;
    }
}
