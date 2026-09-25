package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * The one exception to "lone imps don't fight": an imp that is pressed close by its threat and
 * has failed to flee ({@link ImpEntity#isCornered()}) lands one defensive blow and then dashes
 * to the far side of the threat to break out. Works at any health (a wounded imp still gets to
 * open its own way out) but never returns it to the offensive - once the strike is done the
 * normal flee logic takes over again, with a cooldown before it can strike a second time.
 */
public class ImpCorneredStrikeGoal extends Goal {
    private static final int COOLDOWN = 40;

    private final ImpEntity imp;
    @Nullable
    private LivingEntity target;
    private boolean done;
    private int ticks;

    public ImpCorneredStrikeGoal(ImpEntity imp) {
        this.imp = imp;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.imp.isCornered() || this.imp.getStrikeCooldown() > 0) {
            return false;
        }
        this.target = this.imp.getFleeThreat();
        return this.target != null && this.target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.done && this.ticks < 20 && this.target != null && this.target.isAlive();
    }

    @Override
    public void start() {
        this.done = false;
        this.ticks = 0;
    }

    @Override
    public void stop() {
        this.imp.setStrikeCooldown(COOLDOWN);
        this.imp.setFleeStuckTicks(0);
        this.target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.ticks++;
        this.imp.getLookControl().setLookAt(this.target);
        Vec3 toTarget = this.target.position().add(0.0, this.target.getBbHeight() * 0.5, 0.0).subtract(this.imp.position());
        if (this.imp.getBoundingBox().inflate(0.4).intersects(this.target.getBoundingBox())) {
            this.imp.doHurtTarget(this.target);
            this.imp.startBreakout();
            // Dash through/past the threat and a bit up; flee then picks the far side.
            this.imp.setDeltaMovement(toTarget.normalize().scale(0.65).add(0.0, 0.2, 0.0));
            this.done = true;
        } else {
            this.imp.dash(toTarget, 0.4, 0.6);
        }
    }
}
