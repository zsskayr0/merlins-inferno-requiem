package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.StarvedEntity;

/**
 * Persistent tracking: while it has a target the Starved remembers where it last saw it; when the
 * target is lost (out of sight for too long, or behind cover) it walks to that spot and gives up
 * only once it arrives or {@link #MAX_TICKS} pass.
 */
public class StarvedSeekGoal extends Goal {
    private static final int MAX_TICKS = 400;
    private final StarvedEntity starved;
    private int ticks;

    public StarvedSeekGoal(StarvedEntity starved) {
        this.starved = starved;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.starved.getTarget();
        if (target != null && target.isAlive()) {
            // Keep the memory fresh whenever it's being hunted; nothing to seek yet.
            if (this.starved.hasLineOfSight(target)) {
                this.starved.lastKnownTargetPos = target.position();
            }
            return false;
        }
        return this.starved.lastKnownTargetPos != null;
    }

    @Override
    public void start() {
        this.ticks = 0;
        Vec3 pos = this.starved.lastKnownTargetPos;
        if (pos != null) {
            this.starved.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.1);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.starved.getTarget() == null && this.starved.lastKnownTargetPos != null
                && !this.starved.getNavigation().isDone() && this.ticks < MAX_TICKS;
    }

    @Override
    public void tick() {
        this.ticks++;
    }

    @Override
    public void stop() {
        this.starved.lastKnownTargetPos = null;
        this.starved.getNavigation().stop();
    }
}
