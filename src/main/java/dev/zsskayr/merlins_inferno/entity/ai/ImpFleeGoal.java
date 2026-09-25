package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * Two flavours of running away, at two different priorities (see {@code ImpEntity#registerGoals}):
 * <ul>
 *     <li>{@link Mode#URGENT} (high priority, beats theft and attacks): low health, panic after
 *     losing loot, a failed snatch's retreat, or carrying loot with a threat close by.</li>
 *     <li>{@link Mode#SHY} (below theft/dive): the cowardice of a lone imp or pair - no courage,
 *     so it keeps away from the player. Deliberately low priority so a greedy imp can still
 *     start a snatch on a player standing right there.</li>
 * </ul>
 * While fleeing it counts how long it has failed to gain distance from a close threat; that
 * "stuck" counter is what {@code ImpEntity#isCornered()} reads.
 */
public class ImpFleeGoal extends Goal {
    public enum Mode { URGENT, SHY }

    private static final double URGENT_RANGE = 16.0;
    private static final double CARRIER_RANGE = 12.0;
    private static final double SHY_RANGE = 12.0;
    /** Extra distance beyond the trigger range it keeps running before calming down. */
    private static final double CALM_MARGIN = 4.0;

    private final ImpEntity imp;
    private final Mode mode;
    @Nullable
    private LivingEntity threat;
    @Nullable
    private Vec3 destination;
    private int repathDelay;
    private double lastDistance;
    private double range;
    private int stuck;

    public ImpFleeGoal(ImpEntity imp, Mode mode) {
        this.imp = imp;
        this.mode = mode;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean hasReason() {
        if (this.mode == Mode.URGENT) {
            return this.imp.isLowHealth() || this.imp.getPanicTicks() > 0 || this.imp.getRetreatTicks() > 0 || this.imp.isCarrying();
        }
        return !this.imp.isCourageous() && this.imp.getHesitateTicks() == 0 && !this.imp.isCarrying() && !this.imp.isLowHealth();
    }

    @Override
    public boolean canUse() {
        if (!this.hasReason()) {
            return false;
        }
        boolean calmCarrier = this.imp.isCarrying() && !this.imp.isLowHealth() && this.imp.getPanicTicks() <= 0 && this.imp.getRetreatTicks() <= 0;
        this.range = this.mode == Mode.SHY ? SHY_RANGE : (calmCarrier ? CARRIER_RANGE : URGENT_RANGE);
        LivingEntity found = this.imp.findThreat(this.range);
        if (found == null) {
            return false;
        }
        this.threat = found;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.threat != null && this.threat.isAlive() && this.hasReason()
                && this.imp.distanceTo(this.threat) < this.range + CALM_MARGIN;
    }

    @Override
    public void start() {
        this.imp.setFleeThreat(this.threat);
        this.imp.fearSound();
        this.repathDelay = 0;
        this.stuck = this.imp.getFleeStuckTicks();
        this.lastDistance = this.imp.distanceTo(this.threat);
        this.destination = null;
    }

    @Override
    public void stop() {
        this.imp.setFleeStuckTicks(0);
        this.threat = null;
        this.destination = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.threat == null) {
            return;
        }
        this.imp.setFleeThreat(this.threat);
        double distance = this.imp.distanceTo(this.threat);

        if (--this.repathDelay <= 0) {
            this.repathDelay = 8;
            this.destination = this.imp.pickEscapePoint(this.threat.position(), 10.0);
            if (this.destination == null) {
                this.stuck += 4;
            }
        }
        if (this.destination != null) {
            this.imp.flyToward(this.destination, 0.42);
        } else {
            // Boxed in: at least keep looking at the threat.
            this.imp.getLookControl().setLookAt(this.threat);
        }

        // Not gaining distance while the threat is close = pinned.
        if (distance < 4.0 && distance - this.lastDistance < 0.02) {
            this.stuck++;
        } else {
            this.stuck = Math.max(0, this.stuck - 1);
        }
        this.lastDistance = distance;
        this.imp.setFleeStuckTicks(this.stuck);
    }
}
