package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * The pack's melee attack - a phantom-style dive: {@code PREPARE} (a scream, then hover up and
 * back), {@code DIVE} (fast charge), {@code CURVE} (a slow, low arc past the player - the real
 * opening for a counter-hit) and {@code RETREAT}. Only starts while the imp is courageous, and:
 * <ul>
 *     <li>at most {@value #MAX_SIMULTANEOUS_DIVERS} imps are in the approach (prepare/charge) at once;</li>
 *     <li>dives are staggered - no other imp may have started one in the last {@value #STAGGER_TICKS} ticks;</li>
 *     <li>losing courage (or the group) during PREPARE aborts it; once the charge itself is launched
 *     it always completes ({@link #isInterruptable()}).</li>
 * </ul>
 */
public class ImpDiveGoal extends Goal {
    private enum Phase { PREPARE, DIVE, CURVE, RETREAT }

    public static final int MAX_SIMULTANEOUS_DIVERS = 2;
    private static final int STAGGER_TICKS = 15;
    private static final double SEARCH_RANGE = 24.0;
    private static final TargetingConditions TARGETING = TargetingConditions.forCombat().range(SEARCH_RANGE);

    private final ImpEntity imp;
    @Nullable
    private Player target;
    private Phase phase = Phase.PREPARE;
    private int ticks;
    private boolean hit;
    private boolean finished;
    private double lastDistance;
    @Nullable
    private Vec3 holdSpot;
    @Nullable
    private Vec3 retreatSpot;

    public ImpDiveGoal(ImpEntity imp) {
        this.imp = imp;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.imp.isCourageous() || !this.imp.isAvailableFighter() || this.imp.getDiveCooldown() > 0 || this.imp.getHesitateTicks() > 0) {
            return false;
        }
        if (this.imp.countOtherDivers(SEARCH_RANGE) >= MAX_SIMULTANEOUS_DIVERS) {
            return false;
        }
        if (this.imp.level().getGameTime() - this.imp.latestOtherDiveStart(SEARCH_RANGE) < STAGGER_TICKS) {
            return false;
        }
        this.target = this.imp.level().getNearestPlayer(TARGETING, this.imp);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.finished && this.target != null && this.target.isAlive();
    }

    @Override
    public boolean isInterruptable() {
        return this.phase != Phase.DIVE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.phase = Phase.PREPARE;
        this.ticks = 0;
        this.hit = false;
        this.finished = false;
        this.lastDistance = Double.MAX_VALUE;
        this.retreatSpot = null;
        this.imp.setDiving(true);
        this.imp.markDiveStarted();
        this.imp.screamSound();
        this.holdSpot = this.pickHoldSpot();
    }

    @Override
    public void stop() {
        this.imp.setDiving(false);
        this.imp.setState(ImpEntity.STATE_IDLE);
        this.imp.setDiveCooldown(60 + this.imp.getRandom().nextInt(60));
        this.target = null;
        this.holdSpot = null;
        this.retreatSpot = null;
    }

    private Vec3 pickHoldSpot() {
        Vec3 fromTarget = this.imp.position().subtract(this.target.position()).multiply(1.0, 0.0, 1.0);
        if (fromTarget.lengthSqr() < 1.0E-3) {
            fromTarget = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 eye = this.imp.getEyePosition();
        for (int i = 0; i < 5; i++) {
            Vec3 dir = fromTarget.normalize().yRot((this.imp.getRandom().nextFloat() - 0.5F) * 2.0F);
            double distance = 7.0 + this.imp.getRandom().nextDouble() * 2.0;
            Vec3 spot = this.target.position().add(dir.x * distance, 3.0 + this.imp.getRandom().nextDouble(), dir.z * distance);
            if (this.imp.isClearPath(eye, spot)) {
                return spot;
            }
        }
        return this.imp.position();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.ticks++;
        this.imp.getLookControl().setLookAt(this.target);
        switch (this.phase) {
            case PREPARE -> this.tickPrepare();
            case DIVE -> this.tickDive();
            case CURVE -> this.tickCurve();
            case RETREAT -> this.tickRetreat();
        }
    }

    private void tickPrepare() {
        if (!this.imp.isCourageous() || !this.imp.isAvailableFighter() || this.ticks > 90) {
            this.finished = true; // the group fell apart (or the imp got stuck): drop the attack
            return;
        }
        if (this.holdSpot != null) {
            this.imp.flyToward(this.holdSpot, 0.3);
        }
        boolean inPosition = this.holdSpot == null || this.imp.position().distanceToSqr(this.holdSpot) < 4.0;
        if (this.ticks >= 25 && (inPosition || this.ticks > 50) && this.imp.hasLineOfSight(this.target)) {
            this.phase = Phase.DIVE;
            this.ticks = 0;
            this.imp.setState(ImpEntity.STATE_DIVING);
            this.imp.playAttackAnimation();
        }
    }

    private void tickDive() {
        Vec3 aim = this.target.getEyePosition().subtract(0.0, 0.5, 0.0);
        Vec3 toAim = aim.subtract(this.imp.position());
        this.imp.dash(toAim, 0.5, 0.5);
        if (!this.hit && this.imp.getBoundingBox().inflate(0.4).intersects(this.target.getBoundingBox())) {
            this.hit = true;
            this.imp.doHurtTarget(this.target);
        }
        // Done once it connected, or once it has flown past (distance growing again after a close approach), or timed out.
        double distance = toAim.length();
        boolean passed = distance < 3.0 && distance > this.lastDistance + 0.02;
        this.lastDistance = distance;
        if (this.hit || passed || this.ticks > 40) {
            this.enterCurve();
        }
    }

    private void enterCurve() {
        this.phase = Phase.CURVE;
        this.ticks = 0;
        this.imp.setDiving(false); // no longer counts as "approaching"
    }

    /** The opening: it overshoots on a slow, low arc, well within reach of a swing. */
    private void tickCurve() {
        Vec3 velocity = this.imp.getDeltaMovement();
        double lowY = this.target.getY() + 0.4;
        this.imp.setDeltaMovement(velocity.x * 0.93, (lowY - this.imp.getY()) * 0.1, velocity.z * 0.93);
        if (this.ticks >= 12) {
            this.phase = Phase.RETREAT;
            this.ticks = 0;
            this.imp.setState(ImpEntity.STATE_IDLE);
        }
    }

    private void tickRetreat() {
        if (this.retreatSpot == null || this.ticks % 10 == 0) {
            this.retreatSpot = this.imp.pickEscapePoint(this.target.position(), 9.0);
        }
        if (this.retreatSpot != null) {
            this.imp.flyToward(this.retreatSpot.add(0.0, 2.0, 0.0), 0.35);
        }
        if (this.ticks >= 30) {
            this.finished = true;
        }
    }
}
