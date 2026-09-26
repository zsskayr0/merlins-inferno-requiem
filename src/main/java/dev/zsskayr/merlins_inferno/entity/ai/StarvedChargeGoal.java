package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.StarvedEntity;
import dev.zsskayr.merlins_inferno.registry.ModSounds;

/**
 * Telegraphed dash, available once the Starved is hungry enough. It plants itself and roars for
 * {@link #WINDUP_TICKS} (state WINDUP, visible in the animation), then locks the direction it was
 * facing and runs straight for {@link #CHARGE_TICKS} - sidestepping is the answer. Landing on a
 * target ends the charge with a normal (feeding) hit plus extra knockback.
 */
public class StarvedChargeGoal extends Goal {
    private static final int WINDUP_TICKS = 25;
    private static final int CHARGE_TICKS = 18;
    private static final int COOLDOWN_TICKS = 120;
    private static final double CHARGE_SPEED = 0.85;
    private static final double MIN_RANGE = 5.0;
    private static final double MAX_RANGE = 16.0;

    private final StarvedEntity starved;
    private int cooldown = 40;
    private int phaseTicks;
    private boolean charging;
    private Vec3 direction = Vec3.ZERO;

    public StarvedChargeGoal(StarvedEntity starved) {
        this.starved = starved;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.starved.getTarget();
        if (target == null || !target.isAlive() || this.starved.getHunger() < StarvedEntity.CHARGE_HUNGER
                || !this.starved.onGround()) {
            return false;
        }
        double distance = this.starved.distanceTo(target);
        return distance >= MIN_RANGE && distance <= MAX_RANGE && this.starved.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return this.starved.getTarget() != null && this.starved.getTarget().isAlive()
                && this.phaseTicks < WINDUP_TICKS + CHARGE_TICKS;
    }

    @Override
    public void start() {
        this.phaseTicks = 0;
        this.charging = false;
        this.starved.getNavigation().stop();
        this.starved.setState(StarvedEntity.STATE_WINDUP);
        this.starved.roar();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.starved.getTarget();
        if (target == null) {
            return;
        }
        this.phaseTicks++;
        if (this.phaseTicks <= WINDUP_TICKS) {
            // Track the target while winding up, then lock at the last moment.
            this.starved.getLookControl().setLookAt(target, 60.0F, 60.0F);
            this.starved.setDeltaMovement(this.starved.getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (this.phaseTicks == WINDUP_TICKS) {
                Vec3 delta = target.position().subtract(this.starved.position()).multiply(1.0, 0.0, 1.0);
                this.direction = delta.lengthSqr() < 1.0E-4 ? this.starved.getLookAngle().multiply(1.0, 0.0, 1.0).normalize() : delta.normalize();
            }
            return;
        }
        if (!this.charging) {
            this.charging = true;
            this.starved.setState(StarvedEntity.STATE_CHARGING);
            this.starved.playSound(ModSounds.STARVED_CHARGE.get(), 1.5F, 1.0F);
        }
        Vec3 motion = this.starved.getDeltaMovement();
        this.starved.setDeltaMovement(this.direction.x * CHARGE_SPEED, motion.y, this.direction.z * CHARGE_SPEED);
        this.starved.setYRot((float) (Math.atan2(this.direction.z, this.direction.x) * (180.0 / Math.PI)) - 90.0F);
        this.starved.yBodyRot = this.starved.getYRot();
        this.starved.yHeadRot = this.starved.getYRot();
        if (this.starved.horizontalCollision) {
            this.phaseTicks = WINDUP_TICKS + CHARGE_TICKS; // slammed into a wall: over
        } else if (this.starved.distanceToSqr(target) < 2.6 * 2.6) {
            this.starved.doHurtTarget(target);
            target.push(this.direction.x * 0.8, 0.35, this.direction.z * 0.8);
            target.hurtMarked = true;
            this.phaseTicks = WINDUP_TICKS + CHARGE_TICKS;
        }
    }

    @Override
    public void stop() {
        this.starved.setState(StarvedEntity.STATE_IDLE);
        this.charging = false;
        this.cooldown = COOLDOWN_TICKS;
    }
}
