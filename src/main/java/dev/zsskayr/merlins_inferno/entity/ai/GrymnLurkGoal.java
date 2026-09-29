package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;

import dev.zsskayr.merlins_inferno.entity.GrymnEntity;

/**
 * Idle behaviour: the Grymn drifts to nearby spots just above the floor (never up in the open air), lingering
 * between moves - a predator waiting for prey to wander close. Only runs while it has no target.
 */
public class GrymnLurkGoal extends Goal {
    private static final int MIN_DISTANCE = 4;
    private static final int MAX_DISTANCE = 10;
    private static final int PICK_ATTEMPTS = 6;
    private static final double SPEED = 0.6;

    private final GrymnEntity grymn;

    public GrymnLurkGoal(GrymnEntity grymn) {
        this.grymn = grymn;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.grymn.getTarget() == null && this.grymn.getRandom().nextInt(reducedTickDelay(40)) == 0;
    }

    @Override
    public void start() {
        for (int i = 0; i < PICK_ATTEMPTS; i++) {
            double angle = this.grymn.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = MIN_DISTANCE + this.grymn.getRandom().nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            double x = this.grymn.getX() + Math.cos(angle) * distance;
            double z = this.grymn.getZ() + Math.sin(angle) * distance;
            double ground = this.grymn.groundYBelow(x, this.grymn.getY(), z);
            if (Double.isNaN(ground)) {
                continue;
            }
            double y = ground + GrymnEntity.HOVER_MIN + this.grymn.getRandom().nextDouble() * (GrymnEntity.HOVER_MAX - GrymnEntity.HOVER_MIN);
            if (this.grymn.getNavigation().moveTo(x, y, z, SPEED)) {
                return;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.grymn.getTarget() == null && !this.grymn.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.grymn.getNavigation().stop();
    }
}
